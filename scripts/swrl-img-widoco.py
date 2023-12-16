"""
Copyright (c) 2023 Victor Chavez
This script processes HTML files from the Widoco project and
adds SWRL rule images generated from the aowln-sa project.
Licensed under the GNU General Public License, Version 3 (GPLv3).
SPDX-License-Identifier: GPL-3.0
"""
import argparse
import logging
from bs4 import BeautifulSoup
from pathlib import Path
import shutil
import os

style_sheet_name = "swrl_style.css"

def create_image_container(soup, rule_no, name, part, usename, width):
    """
    Create an image container for SWRL rules.

    Args:
        soup (BeautifulSoup): The BeautifulSoup object representing the HTML content.
        rule_no (int): The rule number.
        name (str): The name obtained from the <h2> tag.
        part (str): The part of the rule ("body" or "head").
        usename (bool): If True, use the name from the <h2> tag for image paths.

    Returns:
        Tag: The container tag with image and text.
    """
    container = soup.new_tag("div", attrs={"class": "swrl-container"})
    if usename:
        image_path = f"swrlrules/rule_{name}-{part}.png"
    else:
        image_path = f"swrlrules/rule_{rule_no}-{part}.png"

    image_tag = soup.new_tag("img", src=image_path, style="width: %dpx; display: inline-block;"%(width),
                             title=f"SWRL {part.capitalize()}")
    text_tag = soup.new_tag("a").string = part.capitalize()
    container.append(text_tag)
    container.append(image_tag)
    return container

def add_swrl_images(html_content, css_filename, usename=False,width=100):
    """
    Add SWRL images to the HTML content.

    Args:
        html_content (str): The HTML content as a string.
        css_filename (str): The CSS filename.
        usename (bool): If True, use the name from the <h2> tag for image paths.

    Returns:
        str: The modified HTML content with added images.
    """
    soup = BeautifulSoup(html_content, 'html.parser')

    existing_images = soup.select("#swrlrules .entity img")
    if existing_images:
        logging.warning("Image tags already exist in the HTML. Skipping. "
                        "Generate Widoco documentation again")
        return None

    entity_divs = soup.select("#swrlrules .entity")

    for i, entity_div in enumerate(entity_divs, start=1):
        name_tag = entity_div.select_one("h3")
        if name_tag:
            name = name_tag.text.strip().replace(" ","_").replace("_to_ToC","")
        else:
            name = "unknown"

        paragraphs = entity_div.select("p")

        for j, paragraph in enumerate(paragraphs, start=1):
            body_container = create_image_container(soup, i, name, "body", usename,width)
            head_container = create_image_container(soup, i, name, "head", usename,width)
            grid_container = soup.new_tag("div", attrs={"class": "grid-container"})
            grid_container.append(body_container)
            grid_container.append(head_container)
            paragraph.insert_after(grid_container)

    logging.info("Image tags, text, and description added to HTML.")
    return str(soup)

def get_script_directory():
    """
    Get the directory of the script.

    Returns:
        Path: The script directory as a Path object.
    """
    return Path(os.path.dirname(os.path.abspath(__file__)))

def process_directory(directory_path, css_filename, usename=False,width=100):
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

        modified_content = add_swrl_images(html_content, css_filename, usename,width)
        if modified_content is None:
            continue

        with open(file_path, 'w', encoding='utf-8') as file:
            file.write(modified_content)

        logging.info("HTML file modified.")

    for index_file_path in directory.glob("index-*.html"):
        logging.info(f"Processing index file: {index_file_path}")

        with open(index_file_path, 'r', encoding='utf-8') as index_file:
            index_content = index_file.read()

        modified_index_content = add_css_link(index_content, css_filename)
        if modified_index_content is None:
            continue

        with open(index_file_path, 'w', encoding='utf-8') as index_file:
            index_file.write(modified_index_content)

        logging.info(f"CSS link added to {index_file_path}")

def add_css_link(html_content, css_filename):
    """
    Add CSS link to the HTML content.

    Args:
        html_content (str): The HTML content as a string.
        css_filename (str): The CSS filename.

    Returns:
        str: The modified HTML content with added CSS link.
    """
    soup = BeautifulSoup(html_content, 'html.parser')

    existing_link = soup.select('link[href$="%s"]' % (style_sheet_name))
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
                        help="Use the name from the <h2> tag for image paths, i.e. the rdfs:label value")
    parser.add_argument("-width", type=int,default=100,
                        help="Max width of swrl images, change if they look too small")
    args = parser.parse_args()

    logging.basicConfig(level=logging.INFO)

    script_dir =  get_script_directory()
    resources_directory = Path(args.directory_path) / "resources"
    resources_directory.mkdir(exist_ok=True)
    shutil.copy(script_dir / style_sheet_name, resources_directory / style_sheet_name)

    process_directory(args.directory_path, style_sheet_name, args.name,args.width)

if __name__ == "__main__":
    main()
