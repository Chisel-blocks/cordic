#!/usr/bin/env bash
#Init submodules in this dir, if any
DIR="$( cd "$( dirname $0 )" && pwd )"
git submodule update --init

#Recursively init submodules
SUBMODULES="\
    "
for module in $SUBMODULES; do
    cd ${DIR}/${module}
    if [ -f "./init_submodules.sh" ]; then
        ./init_submodules.sh
    fi
    sbt publishLocal
    cd ${DIR}
done


exit 0
