#! /usr/bin/env sh

cat README.md \
    | sed '
        s_img/Showcase\_1.png_https://cdn.modrinth.com/data/mws0T3OJ/images/2e57f9236e14dacdb961e7f9461911c4c9b98750.png_;'
