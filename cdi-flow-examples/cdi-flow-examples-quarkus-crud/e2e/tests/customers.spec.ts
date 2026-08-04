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

import { expect, test } from './flow';

test(
  'a customer is created with tags',
  {
    annotation: {
      type: 'description',
      description:
        'Enters a customer with three tags. The service validates it, stores it, asks for a ' +
        'customer-number and fires an event - which is where the audit observer joins the chain.',
    },
  },
  async ({ page }) => {
    await page.goto('/');

    await page.getByTestId('name').fill('Ada Lovelace');
    await page.getByTestId('email').fill('ada@example.org');
    await page.getByTestId('tags').fill('vip, analytics, first');
    await page.getByTestId('save').click();

    await expect(page.getByTestId('row-Ada Lovelace')).toContainText('ada@example.org');
    await expect(page.getByTestId('row-Ada Lovelace')).toContainText('vip, analytics, first');
  },
);

test(
  'a customer without a name is refused',
  {
    annotation: {
      type: 'description',
      description:
        'The validation refuses the customer and the exception travels back out through every ' +
        'frame it passed - which the diagram marks, up to the mapper that turns it into a 422.',
    },
  },
  async ({ page }) => {
    await page.goto('/');

    await page.getByTestId('email').fill('nobody@example.org');
    await page.getByTestId('save').click();

    await expect(page.getByTestId('error')).toHaveText('a customer needs a name');
  },
);

test(
  'a customer is edited',
  {
    annotation: {
      type: 'description',
      description: 'Loads the list, changes the e-mail of an existing customer and stores it again.',
    },
  },
  async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('name').fill('Grace Hopper');
    await page.getByTestId('save').click();
    await expect(page.getByTestId('row-Grace Hopper')).toBeVisible();

    await page.getByTestId('edit-Grace Hopper').click();
    await page.getByTestId('email').fill('grace@example.org');
    await page.getByTestId('save').click();

    await expect(page.getByTestId('row-Grace Hopper')).toContainText('grace@example.org');
  },
);

test(
  'a customer is deleted',
  {
    annotation: {
      type: 'description',
      description: 'Adds a customer and removes it again; the list afterwards no longer holds it.',
    },
  },
  async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('name').fill('Alan Turing');
    await page.getByTestId('save').click();
    await expect(page.getByTestId('row-Alan Turing')).toBeVisible();

    await page.getByTestId('delete-Alan Turing').click();

    await expect(page.getByTestId('row-Alan Turing')).toHaveCount(0);
  },
);
