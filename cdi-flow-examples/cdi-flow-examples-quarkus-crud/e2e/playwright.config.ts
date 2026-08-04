/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineConfig, devices } from '@playwright/test';

const PORT = Number(process.env['E2E_PORT'] ?? 8091);

/**
 * One application, started once, for every use-case in the suite - which is what the label makes
 * possible. Nothing here configures the recorder: the application does that itself.
 */
export default defineConfig({
  testDir: './tests',
  outputDir: './target/test-results',
  // The recorder files a use-case under the name of the test which drove it, and a use-case that
  // ran in parallel with another would still be recorded correctly - but the order in the generated
  // document is the order they arrive in, and one after the other reads better.
  workers: 1,
  timeout: 30_000,
  reporter: [['list']],

  use: {
    baseURL: `http://localhost:${PORT}`,
    ...devices['Desktop Chrome'],
    trace: 'retain-on-failure',
  },

  webServer: {
    command: 'java -jar target/quarkus-app/quarkus-run.jar',
    //from the application's own directory, so its relative output-directory means what it says
    cwd: '..',
    //the application answers on / as soon as it serves the front-end, which needs no extra extension
    url: `http://localhost:${PORT}/`,
    timeout: 120_000,
    reuseExistingServer: false,
    stdout: 'pipe',
    stderr: 'pipe',
    env: {
      QUARKUS_HTTP_PORT: String(PORT),
      //handed on when run.sh was asked for a notation; absent, the application's own choice stands
      ...(process.env['CDI_FLOW_OUTPUT_FORMAT']
        ? { CDI_FLOW_OUTPUT_FORMAT: process.env['CDI_FLOW_OUTPUT_FORMAT'] }
        : {}),
    },
  },
});
