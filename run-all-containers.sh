#!/usr/bin/env bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Runs the identical test-suite against both CDI implementations and keeps the diagrams of both.
#
# Any further arguments are passed on to maven, e.g. ./run-all-containers.sh -o to build offline.
#
set -euo pipefail

cd "$(dirname "$0")"

# cleaned once up front - a clean per container would throw away the diagrams of the previous run
mvn clean "$@"

for container in weld owb; do
    echo
    echo "==============================================================="
    echo "  cdi-flow  ->  ${container}"
    echo "==============================================================="
    mvn install "-P${container}" "$@"
done

echo
echo "==============================================================="
echo "  generated diagrams"
echo "==============================================================="
find cdi-flow-examples -path '*/target/flow-diagrams/*' \( -name '*.mmd' -o -name '*.puml' \) | sort
