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

import { Component, signal } from '@angular/core';

interface Customer {
  id: number;
  name: string;
  email: string;
  tags: string[];
}

/**
 * The whole front-end: a list, a form, edit and delete. Deliberately one component - what this
 * example is about happens on the other side of these four requests.
 *
 * Nothing here knows about cdi-flow. The label a recording run files these requests under is set by
 * the Playwright fixture as a header, and read by the addon's request-filter.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <h1>Customers</h1>

    <form (submit)="save($event)">
      <input data-testid="name" name="name" placeholder="Name" [value]="name()" (input)="name.set($any($event.target).value)" />
      <input data-testid="email" name="email" placeholder="E-mail" [value]="email()" (input)="email.set($any($event.target).value)" />
      <input data-testid="tags" name="tags" placeholder="Tags, comma separated" [value]="tags()" (input)="tags.set($any($event.target).value)" />
      <button data-testid="save" type="submit">{{ editing() === null ? 'Add' : 'Save' }}</button>
      @if (editing() !== null) {
        <button data-testid="cancel" type="button" (click)="resetForm()">Cancel</button>
      }
    </form>

    @if (error()) {
      <p class="error" data-testid="error">{{ error() }}</p>
    }

    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>E-mail</th>
          <th>Tags</th>
          <th></th>
        </tr>
      </thead>
      <tbody data-testid="rows">
        @for (customer of customers(); track customer.id) {
          <tr [attr.data-testid]="'row-' + customer.name">
            <td>{{ customer.name }}</td>
            <td>{{ customer.email }}</td>
            <td>{{ customer.tags.join(', ') }}</td>
            <td>
              <button [attr.data-testid]="'edit-' + customer.name" (click)="edit(customer)">Edit</button>
              <button [attr.data-testid]="'delete-' + customer.name" (click)="remove(customer)">Delete</button>
            </td>
          </tr>
        }
      </tbody>
    </table>
  `,
})
export class App {
  readonly customers = signal<Customer[]>([]);
  readonly error = signal('');
  readonly name = signal('');
  readonly email = signal('');
  readonly tags = signal('');
  readonly editing = signal<number | null>(null);

  constructor() {
    void this.reload();
  }

  async reload(): Promise<void> {
    const response = await fetch('/api/customers');
    this.customers.set(await response.json());
  }

  async save(event: Event): Promise<void> {
    event.preventDefault();
    this.error.set('');
    const id = this.editing();
    const body = JSON.stringify({
      name: this.name(),
      email: this.email(),
      tags: this.tags()
        .split(',')
        .map((tag) => tag.trim())
        .filter((tag) => tag.length > 0),
    });

    const response = await fetch(id === null ? '/api/customers' : `/api/customers/${id}`, {
      method: id === null ? 'POST' : 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body,
    });

    if (response.status === 422) {
      this.error.set((await response.json()).message);
      return;
    }
    this.resetForm();
    await this.reload();
  }

  edit(customer: Customer): void {
    this.editing.set(customer.id);
    this.name.set(customer.name);
    this.email.set(customer.email ?? '');
    this.tags.set(customer.tags.join(', '));
  }

  async remove(customer: Customer): Promise<void> {
    await fetch(`/api/customers/${customer.id}`, { method: 'DELETE' });
    await this.reload();
  }

  resetForm(): void {
    this.editing.set(null);
    this.name.set('');
    this.email.set('');
    this.tags.set('');
    this.error.set('');
  }
}
