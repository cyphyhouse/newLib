#!/bin/bash

directoryd -d
./bin/test_query
python generator.py
chmod +x launch.sh
./launch.sh
