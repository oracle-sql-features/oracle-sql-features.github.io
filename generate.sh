#!/bin/bash

echo "🔄 Generating navigation"
java .github/scripts/generate_navigation.java "$(pwd)"

echo ""
echo "🛠️  Building site"
echo "Please wait..."
antora local-playbook.yml
