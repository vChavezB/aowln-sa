# AOWLN SA

The Aided Owl Notation Standalone utility is a java project that allows to generate SWRL rule images from ontologies.
This work is based on https://github.com/KITE-Cloud/AOWLN and has been expanded to be a standalone application.
This means that it can be used without Protégé to generate SWRL rules automatically of an OWL ontology as a jar application
or as part of another java project.

# Installation

The project can be used as a standalone java application or as part of another project

## Standalone application

To use it as a standalone application, download the latest released [package](https://github.com/vChavezB/aowln-sa/packages) with dependencies. 

### Usage

```bash
java -jar aowln-sa-X.Y.Z-jar-with-dependencies.jar MyOntology.rdf OutputDir
```

Where:
- `X.Y.Z`: aowln-sa semantic versioning
- `MyOntology.rdf`: path to the serialized ontology
- `OutputDir`: Output directory for the SWRL rules as images.
- 
The output will generate the pattern 

- rule_`X`-`body`.png
- rule_`X`-`head`.png

where:
- `X`: rule number ordered from declaration in the ontology
- `head`: Head of the SWRL rule (i.e. after ->)
- `body`: Body of the SWRL rule (i.e. before ->)

## Java project

## Maven

[![](https://jitpack.io/v/vChavezB/aowln-sa.svg)](https://jitpack.io/#vChavezB/aowln-sa)

Add dependency to `pom.xml`:

```yml
<dependencies>
 <dependency>
    <groupId>com.github.vchavezb</groupId>
    <artifactId>aowln-sa</artifactId>
    <version>0.0.7</version>
  </dependency>
</dependencies>
```

Include github repository to `pom.xml`:

```yml
 <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

## Usage

Check the directory `src\test\java\aowln` for a simple example on how to load and produce
images with the `AOWLNServiceFacade` class.

# Integration with Widoco

The swrl images generated by aowln-sa can be integrated into projects that use the Widoco
documentation project. To use it do the following.

1. Create widoco documentation.
3. Create swrl images with aowln-sa
    ```bash
    java -jar aowln-sa-X.Y.Z-jar-with-dependencies.jar MyOntology.ttl YOUR_WIDOCO_DOC_PATH/swrlrules -name
    ```
   where:
   - YOUR_WIDOCO_DOC_PATH: Path where Widoco documentation was generated
2. Run awoln widoco script
    ```bash
    python swrl-img-widoco.py YOUR_WIDOC_DOC_PATH -height 100
    ```
   Notes:
   - If the images result too small in the html visualization, change the optional
   parameter `-height`, which modifies the maximum height for images in the html website.
   - Requires python 3



