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
# Renders the generated diagrams to PNG - Mermaid (*.mmd) via the mermaid-cli image, PlantUML
# (*.puml) via the plantuml image.
#
#   ./render-diagrams.sh              # the showcase directories of both containers
#   ./render-diagrams.sh --all        # every diagram the test-suite produced
#   ./render-diagrams.sh <directory>  # every diagram below the given directory
#
# All files below one directory are rendered inside a single container run - starting a container
# (and a headless browser, respectively a JVM) per file would take far longer than the rendering.
#
set -euo pipefail

cd "$(dirname "$0")"

MERMAID_IMAGE="${MERMAID_IMAGE:-ghcr.io/mermaid-js/mermaid-cli/mermaid-cli}"
PLANTUML_IMAGE="${PLANTUML_IMAGE:-docker.io/plantuml/plantuml}"
RUNNER="${CONTAINER_RUNTIME:-podman}"
DIAGRAM_ROOT="cdi-flow-examples"

count_of() {
    find "$1" -name "*.$2" | wc -l | tr -d ' '
}

render_mermaid() {
    local absolute="$1"
    local count
    count="$(count_of "${absolute}" mmd)"
    [[ "${count}" == "0" ]] && return 0

    echo "  ${count} mermaid diagram(s) ..."
    "${RUNNER}" run --rm --entrypoint /bin/sh -v "${absolute}:/data:Z" "${MERMAID_IMAGE}" -c '
        find /data -name "*.mmd" | sort | while read -r diagram; do
            /home/mermaidcli/node_modules/.bin/mmdc \
                -p /puppeteer-config.json \
                -i "${diagram}" -o "${diagram%.mmd}.png" \
                -b white -s 2 >/dev/null || echo "FAILED: ${diagram}"
        done'
}

render_plantuml() {
    local absolute="$1"
    local count
    count="$(count_of "${absolute}" puml)"
    [[ "${count}" == "0" ]] && return 0

    echo "  ${count} plantuml diagram(s) ..."
    # PlantUML writes the PNG next to its source and recurses on its own
    "${RUNNER}" run --rm -v "${absolute}:/data:Z" "${PLANTUML_IMAGE}" \
        -tpng -nometadata -failfast2 "/data/**.puml"
}

render_directory() {
    local directory="$1"
    local absolute
    absolute="$(cd "${directory}" && pwd)"

    if [[ "$(count_of "${absolute}" mmd)" == "0" && "$(count_of "${absolute}" puml)" == "0" ]]; then
        echo "no diagrams below ${directory}"
        return
    fi

    echo "rendering below ${directory} ..."
    render_mermaid "${absolute}"
    render_plantuml "${absolute}"
}

if [[ ! -d "${DIAGRAM_ROOT}" ]]; then
    echo "no diagrams yet - run ./run-all-containers.sh first" >&2
    exit 1
fi

case "${1:-}" in
    --all)
        # one container run per example-module keeps the mermaid/plantuml split intact
        while read -r moduleDiagrams; do
            render_directory "${moduleDiagrams}"
        done < <(find "${DIAGRAM_ROOT}" -type d -name flow-diagrams | sort)
        ;;
    "")
        # the showcase of every example, whichever notation it produces
        while read -r showcase; do
            render_directory "${showcase}"
        done < <(find "${DIAGRAM_ROOT}" -type d -path '*/flow-diagrams/*/showcase' | sort)
        ;;
    *)
        render_directory "$1"
        ;;
esac

echo
echo "rendered images: $(find "${DIAGRAM_ROOT}" -path '*/flow-diagrams/*' -name '*.png' | wc -l | tr -d ' ')"
find "${DIAGRAM_ROOT}" -path '*/flow-diagrams/*' -name '*.png' | sort
