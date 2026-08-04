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

import { test as base } from '@playwright/test';

/**
 * The entire integration on the Playwright side: every request a test makes carries the name of
 * that test, and the addon files everything recorded during those requests under it.
 *
 * A test may annotate itself with a `description`, which ends up above its diagram in the generated
 * document. Nothing else is needed - no reporter, no per-test app restart, no output-directory per
 * use-case.
 */
export const test = base.extend({
  context: async ({ context }, use, testInfo) => {
    const description = testInfo.annotations.find(
      (annotation) => annotation.type === 'description',
    )?.description;

    await context.setExtraHTTPHeaders({
      'X-Flow-Label': testInfo.title,
      ...(description ? { 'X-Flow-Description': description } : {}),
    });
    await use(context);
  },
});

export { expect } from '@playwright/test';
