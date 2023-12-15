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

Where the first argument is the path to the serialized ontology and the second is the output directory for the 
SWRL rules as images.

## Java project

## Maven

Add dependency to `pom.xml`:

```yml
<dependencies>
 <dependency>
    <groupId>com.github.vchavezb</groupId>
    <artifactId>aowln-sa</artifactId>
	<version>0.06</version>
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



