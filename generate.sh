#!/bin/bash

echo "🔄 Generating navigation"
java .github/scripts/generate_navigation.java "$(pwd)"

echo ""
echo "🛠️  Building site"
echo "Please wait..."
npx antora local-playbook.yml
