'use strict'

const CATEGORIES = "categories/pages"
const VERSIONS   = "versions/pages"
const INDEX      = "index.adoc"

module.exports.register = function () {
  this
    .on('contentAggregated', ({ contentAggregate }) => {
        contentAggregate.forEach(({ name, title, version, nav, files }) => {
          files.forEach((file) => {
              if (file.src.basename == INDEX) return
              if (file.src.path.substring(CATEGORIES) ||
                  file.src.path.substring(VERSIONS)) {
                file.src.editUrl = file.src.origin.webUrl + "/blob/" +
                                   file.src.origin.refname + "/features/" +
                                   file.src.basename
              }
          })
        })
    })
}