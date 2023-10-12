#!/bin/bash

# Dynamically determine path to `pre-push` script
PROPERTIES_PATH="$(dirname "$0")/pre-push"
GIT_HOOKS_PATH="$(dirname "$0")/../.git/hooks"

# Copy `pre-push` script to `.git/hooks/pre-push`
cp $PROPERTIES_PATH $GIT_HOOKS_PATH/pre-push

# Grant execute permissions to `pre-push` script
chmod +x $GIT_HOOKS_PATH/pre-push
