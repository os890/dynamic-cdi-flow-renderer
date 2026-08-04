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
# The same script as the Quarkus example has, against the same application on Weld: build, drive the
# use-cases through a browser, and point at what was recorded.
#
#   ./run.sh                          # Mermaid, the default
#   ./run.sh --format plantuml        # the same recordings in the other notation
#   ./run.sh --grep deleted           # anything else is handed to Playwright
#
# There is nothing about recording in here beyond that one notation: the portable extension in the
# addon jar attaches the recorder and arms it, the suite labels its requests, and the addon does the
# rest - one directory per use-case, the diagram of each, and the document listing all of them.
#
set -euo pipefail

cd "$(dirname "$0")"

usage() {
    cat <<'USAGE'
usage: ./run.sh [--format mermaid|plantuml] [playwright arguments...]

  --format mermaid    record Mermaid diagrams - the default, and what the application configures
  --format plantuml   record the same flows in PlantUML notation instead
  anything else       handed to Playwright, e.g. --grep deleted
USAGE
}

format=""
playwright_arguments=()

while [ $# -gt 0 ]; do
    case "$1" in
    --format | -f)
        [ $# -ge 2 ] || {
            echo "--format needs a value: mermaid or plantuml" >&2
            exit 2
        }
        format="$2"
        shift 2
        ;;
    --format=*)
        format="${1#--format=}"
        shift
        ;;
    --help | -h)
        usage
        exit 0
        ;;
    *)
        playwright_arguments+=("$1")
        shift
        ;;
    esac
done

# The addon accepts the short forms as well; they are spelled out here so that a typo fails now
# rather than being recorded in the wrong notation with a warning nobody reads.
case "$(printf '%s' "$format" | tr '[:upper:]' '[:lower:]')" in
"")
    # left to the application's own configuration, which asks for Mermaid
    ;;
mermaid | mmd)
    export CDI_FLOW_OUTPUT_FORMAT=mermaid
    ;;
plantuml | puml | uml)
    export CDI_FLOW_OUTPUT_FORMAT=plantuml
    ;;
*)
    echo "unknown format '$format' - use mermaid or plantuml" >&2
    exit 2
    ;;
esac

echo "recording as ${CDI_FLOW_OUTPUT_FORMAT:-mermaid}"

mvn -q package -DskipTests
(
    cd e2e
    pnpm install --frozen-lockfile --ignore-scripts
    npx playwright test ${playwright_arguments[@]+"${playwright_arguments[@]}"}
)

echo
echo "recorded use-cases: $(pwd)/target/flow-diagrams/use-cases.md"
ls target/flow-diagrams
