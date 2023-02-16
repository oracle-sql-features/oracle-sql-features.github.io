#!/bin/bash

PWD=$(pwd)
FEATURES="${PWD}/docs/modules/features/partials"
CATEGORIES="${PWD}/docs/modules/categories"
VERSIONS="${PWD}/docs/modules/versions"
GIT_COMMIT=""

echo "* xref:index.adoc[]" > "${CATEGORIES}/nav.adoc"
echo "* xref:index.adoc[]" > "${VERSIONS}/nav.adoc"

for page in $(ls ${FEATURES})
do
    echo "➡️  Processing ${page}"
    DATABASE_CATEGORY=$(grep ":database-category:" "${FEATURES}/${page}" | awk '{print $2}')
    DATABASE_VERSION=$(grep ":database-version:" "${FEATURES}/${page}" | awk '{print $2}')

    # create category page if it does not exist
    if [ ! -d "${CATEGORIES}/pages/${DATABASE_CATEGORY}" ];
    then
        mkdir "${CATEGORIES}/pages/${DATABASE_CATEGORY}"
        echo "= ${DATABASE_CATEGORY}\n" > "${CATEGORIES}/pages/${DATABASE_CATEGORY}/index.adoc"
        echo "* xref:${DATABASE_CATEGORY}/index.adoc[]" >> "${CATEGORIES}/pages/index.adoc"
        git add "${CATEGORIES}/pages/${DATABASE_CATEGORY}/index.adoc"
        GIT_COMMIT="true"
    fi

    # create version page if it does not exist
    if [ ! -d "${VERSIONS}/pages/${DATABASE_VERSION}" ];
    then
        mkdir "${VERSIONS}/pages/${DATABASE_VERSION}"
        echo "= ${DATABASE_VERSION}\n" > "${VERSIONS}/pages/${DATABASE_VERSION}/index.adoc"
        echo "* xref:${DATABASE_VERSION}/index.adoc[]" >> "${VERSIONS}/pages/index.adoc"
        git add "${VERSIONS}/pages/${DATABASE_VERSION}/index.adoc"
        GIT_COMMIT="true"
    fi

    # create pages
    echo "include::features:partial\$${page}[]" > "${CATEGORIES}/pages/${DATABASE_CATEGORY}/${page}"
    echo "include::features:partial\$${page}[]" > "${VERSIONS}/pages/${DATABASE_VERSION}/${page}"

    # update navs
    CATEGORY_EXISTS=$(grep "${DATABASE_CATEGORY}/index.adoc" "${CATEGORIES}/nav.adoc")
    VERSION_EXISTS=$(grep "${DATABASE_VERSION}/index.adoc" "${VERSIONS}/nav.adoc")

    if [ -z "${CATEGORY_EXISTS}" ];
    then
      echo "** xref:${DATABASE_CATEGORY}/index.adoc[]" >> "${CATEGORIES}/nav.adoc"
    fi
    echo "*** xref:${DATABASE_CATEGORY}/${page}[]" >> "${CATEGORIES}/nav.adoc"

    if [ -z "${VERSION_EXISTS}" ];
    then
      echo "** xref:${DATABASE_VERSION}/index.adoc[]" >> "${VERSIONS}/nav.adoc"
    fi
    echo "*** xref:${DATABASE_VERSION}/${page}[]" >> "${VERSIONS}/nav.adoc"
done

if [ -n "${GIT_COMMIT}" ];
then
    git config --global user.email "41898282+github-actions[bot]@users.noreply.github.com"
    git config --global user.name "GitHub Action"
    git push origin main
fi