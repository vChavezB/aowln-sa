"""
Copyright (c) 2023 Victor Chavez
This script processes HTML files from the Widoco project and
adds SWRL rule images generated from the aowln-sa project.
Licensed under the GNU General Public License, Version 3 (GPLv3).
SPDX-License-Identifier: GPL-3.0
"""
import argparse
import logging
import pathlib

from bs4 import BeautifulSoup
from pathlib import Path
import shutil
import os
import xml.etree.ElementTree as ET

style_sheet_name = "swrl-image.css"
java_script_name = "swrl-img-to-svg.js"

ET.register_namespace('', "http://www.w3.org/2000/svg")


def get_script_directory():
    """
    Get the directory of the script.

    Returns:
        Path: The script directory as a Path object.
    """
    return Path(os.path.dirname(os.path.abspath(__file__)))


class SVG:
    class size:
        def __init__(self, width, height):
            self.width = width
            self.height = height

    def __init__(self, file_path):
        self.height = None
        self.width = None
        self.file_path = file_path
        self.get_size()

    def get_size(self):
        # Parse the SVG file
        tree = ET.parse(self.file_path)
        root = tree.getroot()
        # Extract width and height attributes
        width = float(root.attrib.get('width', None).replace("px", ""))
        height = float(root.attrib.get('height', None).replace("px", ""))

        size = SVG.size(width, height)
        self.width = size.width
        self.height = size.height

    def set_size(self, width, height):
        # Parse the SVG file
        tree = ET.parse(self.file_path)
        root = tree.getroot()

        # Remove the namespace from the root element
        root.tag = 'svg'

        # Update width and height attributes without a namespace
        root.set('width', str(width) + "px")
        root.set('height', str(height) + "px")

        # Save the modified SVG to the same file
        tree.write(self.file_path, encoding="utf-8", xml_declaration=True)

        self.width = width
        self.height = height


class WidocoSWRL:
    def __init__(self, widoco_path: str, useName: bool, maxHeight: int):
        self.widocoPath = pathlib.Path(widoco_path)
        self.resources_dir = self.widocoPath / "resources"
        self.useName = useName
        self.maxHeight = maxHeight
        self.resources_dir.mkdir(exist_ok=True)
        self.ruleNames = []
        self.get_rules = True
        shutil.copy(get_script_directory() / style_sheet_name, self.resources_dir / style_sheet_name)
        shutil.copy(get_script_directory() / java_script_name, self.resources_dir / java_script_name)

    def create_image_container(self, soup, rule_no, name, part):
        """
        Create an image container for SWRL rules.

        Args:
            soup (BeautifulSoup): The BeautifulSoup object representing the HTML content.
            rule_no (int): The rule number.
            name (str): The name obtained from the <h2> tag.
            part (str): The part of the rule ("body" or "head").
            usename (bool): If True, use the name from the <h2> tag for image paths.
        """
        container = soup.new_tag("div", attrs={"class": "swrl-container"})
        if self.useName:
            image_path = f"swrlrules/rule_{name}-{part}.svg"
        else:
            image_path = f"swrlrules/rule_{rule_no}-{part}.svg"
        logging.info(f"Adding image path {image_path}")
        image_tag = soup.new_tag("img", src=image_path, id=part, width="auto",
                                 title=f"SWRL {part.capitalize()}")
        text_tag = soup.new_tag("a").string = part.capitalize()
        container.append(text_tag)
        container.append(image_tag)
        return container

    def scaleSVG(self, rule_name):
        body_svg_path = f"swrlrules/rule_{rule_name}-body.svg"
        head_svg_path = f"swrlrules/rule_{rule_name}-head.svg"
        body = SVG(self.widocoPath / body_svg_path)
        head = SVG(self.widocoPath / head_svg_path)
        body_ratio = body.width / body.height
        head_ratio = head.width / head.height
        scaleFactor = self.maxHeight / max(body.height, body.height)
        # Minimum allowed scaled
        minScale = 0.12
        # used in case image is caled below minScale
        heightComp = 300
        if scaleFactor < minScale:
            scaleFactor = (self.maxHeight + heightComp) / max(body.height, head.height)
        new_body_height = body.height * scaleFactor
        new_body_width = new_body_height * body_ratio
        new_head_height = head.height * scaleFactor
        new_head_width = new_head_height * head_ratio
        body.set_size(new_body_width, new_body_height)
        head.set_size(new_head_width, new_head_height)

    def add_swrl_images(self, html_content):
        """
        Add SWRL images to the HTML content.

        Args:
            html_content (str): The HTML content as a string.
            usename (bool): If True, use the name from the <h2> tag for image paths.

        Returns:
            str: The modified HTML content with added images.
        """
        soup = BeautifulSoup(html_content, 'html.parser')

        existing_images = soup.select("#swrlrules .entity img")
        logging.info("Adding img tags to Widoco crossref")
        if existing_images:
            logging.warning("Image tags already exist in the HTML. Skipping, "
                            "Generate Widoco documentation again")

        entity_divs = soup.select("#swrlrules .entity")

        for i, entity_div in enumerate(entity_divs, start=1):
            name_tag = entity_div.select_one("h3")
            if name_tag:
                name = name_tag.text.strip().replace(" ", "_").replace("back_to_ToC_or_SWRL_ToC", "")
            else:
                name = "unknown"

            paragraphs = entity_div.select("p")

            for j, paragraph in enumerate(paragraphs, start=1):
                if self.get_rules:
                    self.ruleNames.append(name)
                if not existing_images:
                    body_container = self.create_image_container(soup, i, name, "body")
                    head_container = self.create_image_container(soup, i, name, "head")
                    grid_container = soup.new_tag("div", attrs={"class": "grid-container"})
                    grid_container.append(body_container)
                    grid_container.append(head_container)
                    paragraph.insert_after(grid_container)
        if self.get_rules:
            self.get_rules = False

        return str(soup)

    def process_directory(self, directory_path, css_filename):
        """
        Process all HTML files in the specified directory.

        Args:
            directory_path (str): The path to the directory containing HTML files.
            css_filename (str): The CSS filename.
            usename (bool): If True, use the name from the <h2> tag for image paths.
        """
        directory = Path(directory_path)
        sections_directory = directory / "sections"

        for file_path in sections_directory.glob("crossref-*.html"):
            logging.info(f"Processing file: {file_path}")

            with open(file_path, 'r', encoding='utf-8') as file:
                html_content = file.read()

            modified_content = self.add_swrl_images(html_content)
            if modified_content is None:
                continue

            with open(file_path, 'w', encoding='utf-8') as file:
                file.write(modified_content)

        logging.info("Scaling SVG rules")
        for rule in self.ruleNames:
            self.scaleSVG(rule)

        for index_file_path in directory.glob("index-*.html"):
            logging.info(f"Processing index file: {index_file_path}")

            with open(index_file_path, 'r', encoding='utf-8') as index_file:
                index_content = index_file.read()

            modified_index_content = self._add_css_link(index_content, css_filename)
            if modified_index_content is None:
                continue

            modified_index_content = self._add_scripts(modified_index_content)
            modified_index_content = self._add_loadEvent(modified_index_content)

            with open(index_file_path, 'w', encoding='utf-8') as index_file:
                index_file.write(modified_index_content)

            logging.info(f"CSS link added to {index_file_path}")

    def _add_loadEvent(self, html_content):
        """
        Add document load event in TOC
        This allows to load the swrl-image-scale java script
        to adjust images since Widoco loads crossref section externally
        Args:
            html_content:

        Returns:

        """
        # Parse the HTML content with BeautifulSoup
        soup = BeautifulSoup(html_content, 'html.parser')

        # Find the script with the specific content
        target_script = soup.find('script', string=lambda x: 'loadTOC();' in x if x else False)

        if target_script:
            # Find the loadTOC() string within the script content
            loadTOC_string = 'loadTOC();'

            # Find the position of the loadTOC() string within the script content
            loadTOC_position = target_script.text.find(loadTOC_string)

            if loadTOC_position != -1:
                # Insert a new line after the loadTOC() method call
                target_script.contents[0].replace_with(
                    target_script.contents[0][:loadTOC_position + len(loadTOC_string)] +
                    '\t\ndocument.dispatchEvent(new Event("DOMContentLoaded"));' +
                    target_script.contents[0][loadTOC_position + len(loadTOC_string):]
                )

        # Return the updated HTML content
        return str(soup)

    def _add_scripts(self, html_content):
        """
        Add the java scripts to the html
        Args:
            html_content:

        Returns:

        """
        # Parse the HTML content with BeautifulSoup
        soup = BeautifulSoup(html_content, 'html.parser')

        # Create the script tag
        script_tag = soup.new_tag('script', src=f'resources/{java_script_name}', defer=True)
        query_script = soup.new_tag('script', src=f'resources/jquery.min.js', defer=True)

        # Append the script tag to the head or body (adjust as needed)
        head_tag = soup.head
        if not head_tag:
            head_tag = soup.new_tag('head')
            soup.html.insert(0, head_tag)

        head_tag.append(script_tag)
        head_tag.append(query_script)

        # Return the updated HTML content
        return str(soup)

    @staticmethod
    def _add_css_link(html_content, css_filename):
        """
        Add CSS link to the HTML content.

        Args:
            html_content (str): The HTML content as a string.
            css_filename (str): The CSS filename.

        Returns:
            str: The modified HTML content with added CSS link.
        """
        soup = BeautifulSoup(html_content, 'html.parser')

        existing_link = soup.select('link[href$="%s"]' % style_sheet_name)
        if existing_link:
            logging.warning("CSS link already exists in the HTML. Skipping.")
            return None

        head_tag = soup.find('head')
        if not head_tag:
            head_tag = soup.new_tag('head')
            soup.html.insert(0, head_tag)

        link_tag = soup.new_tag("link", rel="stylesheet", type="text/css", href=f"resources/{css_filename}")
        head_tag.append(link_tag)

        logging.info("CSS link added to HTML.")
        return str(soup)


def main():
    """
    Main function to execute the script.
    """
    parser = argparse.ArgumentParser(description="Add SWRL images to HTML files.")
    parser.add_argument("directory_path", help="Path to the directory containing HTML files.")
    parser.add_argument("-name", action="store_true",
                        help="Use the name from the <h2> tag for image paths, i.e. the rdfs:label from the" +
                             "swrl rules\nIf not set the img path will be automatically generated in the order\n" +
                             "of appearance of the swrl rules in the crossref-xx.html page with format swrlrules/rule_{rule_no}-{part}.png" +
                             "Use this option if the images do not correspond to the rule, typically this happens when the order\n" +
                             "of the rules in the serialized ontology does not correspond to the one made by Widoco")
    parser.add_argument("-height", type=int, default=100,
                        help="Max height of swrl images, change if they look too small")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO)

    widoco_swrl = WidocoSWRL(args.directory_path, args.name, args.height)
    widoco_swrl.process_directory(args.directory_path, style_sheet_name, )


if __name__ == "__main__":
    main()
