# AOWLN SA

The Aided Owl Notation Standalone utility is a java project that allows to generate SWRL rule images from ontologies.
This work is based on https://github.com/KITE-Cloud/AOWLN and has been expanded to be a standalone application.
This means that it can be used without Protégé to generate SWRL rules automatically of an OWL ontology as a jar application
or as part of another java project.

# How to Use

The project can be used as a standalone java application or as part of another project

## Standalone application

To use it as a standalone application, download the latest released jar package and execute it as follows

```bash
java -jar aowln-sa.jar MyOntology.rdf OutputDir
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
  </dependency>
</dependencies>
```

Include github repository to `pom.xml`:

```yml
  <repositories>
        <repository>
            <id>github</id>
            <name>GitHub VChavezB Apache Maven Packages</name>
            <url>https://maven.pkg.github.com/vchavezb/*</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
```



