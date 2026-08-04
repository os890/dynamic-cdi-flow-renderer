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
set -euo pipefail

cd "$(dirname "$0")"

mvn -q package -DskipTests
(cd e2e && pnpm install --frozen-lockfile --ignore-scripts && npx playwright test "$@")

echo
echo "recorded use-cases: $(pwd)/target/flow-diagrams/use-cases.md"
ls target/flow-diagrams
