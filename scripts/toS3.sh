#!/bin/bash
scljs release app
aws s3 cp public/index.html s3://puttest
aws s3 cp target/js/main.js s3://puttest/js/main.js
