#!/bin/bash

echo "🔄 Generating navigation"
sh .github/scripts/generate-navigation.sh

echo ""
echo "🛠️  Building site"
echo "Please wait..."
antora local-playbook.yml
