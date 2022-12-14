#!/bin/bash

PWD=`pwd`
FEATURES="${PWD}/docs/modules/features/partials"
CATEGORIES="${PWD}/docs/modules/categories"
VERSIONS="${PWD}/docs/modules/versions"

echo "* xref:index.adoc[]" > ${CATEGORIES}/nav.adoc
echo "* xref:index.adoc[]" > ${VERSIONS}/nav.adoc

for page in `ls ${FEATURES}`
do
    echo "➡️  Processing ${page}"
    DATABASE_CATEGORY=`grep ":database-category:" ${FEATURES}/${page} | awk '{print $2}'`
    DATABASE_VERSION=`grep ":database-version:" ${FEATURES}/${page} | awk '{print $2}'`

    # create pages
    echo "include::features:partial\$${page}[]" > ${CATEGORIES}/pages/${DATABASE_CATEGORY}/${page}
    echo "include::features:partial\$${page}[]" > ${VERSIONS}/pages/${DATABASE_VERSION}/${page}

    # update navs
    CATEGORY_EXISTS=`grep ${DATABASE_CATEGORY}/index.adoc ${CATEGORIES}/nav.adoc`
    VERSION_EXISTS=`grep ${DATABASE_VERSION}/index.adoc ${VERSIONS}/nav.adoc`

    if [ -z "${CATEGORY_EXISTS}" ];
    then
      echo "** xref:${DATABASE_CATEGORY}/index.adoc[]" >>  ${CATEGORIES}/nav.adoc
    fi
    echo "*** xref:${DATABASE_CATEGORY}/${page}[]" >>  ${CATEGORIES}/nav.adoc

    if [ -z "${VERSION_EXISTS}" ];
    then
      echo "** xref:${DATABASE_VERSION}/index.adoc[]" >>  ${VERSIONS}/nav.adoc
    fi
    echo "*** xref:${DATABASE_VERSION}/${page}[]" >>  ${VERSIONS}/nav.adoc
done