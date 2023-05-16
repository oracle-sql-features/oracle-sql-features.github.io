# Oracle SQL Features

## Contributing

To contribute a feature to this page, add a new [AsciiDoc](https://asciidoc.org/) (`*.adoc`)
file into the `features` folder.  
You can find an AsciiDoc template `feature.adoctemplate` in the `features` folder.

AsciiDoc files can be organized into subfolders.

For more information about the AsciiDoc syntax, see the [AsciiDoc Syntax Quick Reference](https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/).

## Building the website locally

It is encouraged to build the website locally first before pushing to GitHub to
detect rendering issues or typos before the changes go live.

### Dependencies

1. Install Antora as explained at https://docs.antora.org/antora/latest/install-and-run-quickstart/
1. Install Java 11 or greater https://www.oracle.com/java/technologies/downloads/

### Build

1. Run the `generate.sh` script
    ```shell
    $ sh generate.sh
    ```
1. Inspect the generated site at `$PWD/build/site/index.html`
