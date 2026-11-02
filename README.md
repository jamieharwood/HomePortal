# Home Portal

Home Portal is a small web application for keeping a list of URLs, opening the selected one beside that list, and changing the order in which the links appear. The list stays on screen while a link is open.

## Features

- Add an absolute `http` or `https` URL, with an optional label.
- Remove a link after a confirmation.
- Rename a link without changing its URL.
- Reorder links by dragging the handle, with the up and down buttons, or with Alt+Arrow Up and Alt+Arrow Down.
- Keep the link list visible on desktop and on a phone. On a narrow screen the list sits above the open page.
- Remember the last selected link in this browser.
- Store links in MongoDB so they survive a restart.
- Seed three starter links the first time the collection is empty.

## Built with

- Java 25
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data MongoDB
- Gradle 9.8.2

The page is static HTML, CSS, and JavaScript. It talks to the app through `/api/links`.

## Prerequisites

- JDK 25. The Gradle toolchain requests Java 25 and can download it through the Foojay resolver when the wrapper runs.
- Network access to MongoDB at `storenator.bluelarma.com:27017`. The app connects without a username or password.
- A browser for the portal page.

## Getting started

Clone the repository and start the app from the project root:

```bash
git clone https://github.com/jamieharwood/HomePortal.git
cd HomePortal
./gradlew bootRun
```

In IntelliJ, open the Gradle project and run `com.bluelarma.homeportal.HomePortalApplication`.

When the app has started, open the portal:

```text
http://localhost:8080/
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Configuration

Settings live in `src/main/resources/application.properties`.

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | Port for the portal and the API |
| `spring.mongodb.host` | `storenator.bluelarma.com` | MongoDB host |
| `spring.mongodb.port` | `27017` | MongoDB port |
| `spring.mongodb.database` | `homeportal` | Database for portal links |
| `spring.data.mongodb.auto-index-creation` | `true` | Creates the unique index on `url` |

Links are stored in the `portal_links` collection. Each document has an id, a URL, a label, and a position.

The app does not print exception messages, stack traces, or binding errors in HTTP error responses.

## Usage

The link list is the rail on the left (or the top on a phone). Choose a row to open that URL in the page. Use **Open in new tab** when you want the site outside the portal, and **Reload** to load the framed page again.

**Add URL** appends a link. A blank label is filled in from the URL: the host name before the first dot, capitalized, plus the port when one is set (`Storenator 8081`) or the path when there is no port (`Example/docs`).

URLs are stored in a normalized form:

- The scheme and host are lowercased.
- An empty path becomes `/`.
- The user info and the fragment are dropped.
- Only absolute `http` and `https` URLs are accepted.
- The same URL cannot be added twice.

The first launch with an empty `portal_links` collection inserts these links, in this order:

1. `http://storenator.bluelarma.com:8081/`
2. `http://storenator.bluelarma.com:8082/`
3. `http://storenator.bluelarma.com:8083/`

Later launches leave existing links alone.

The selected link is stored in the browser under `homeportal.selectedId`. It is not stored in MongoDB.

## API

The page and any same-origin client use these endpoints. A mutating request that sends `Sec-Fetch-Site` with a value other than `same-origin` or `none` receives `403` and `{"message":"Cross-site changes are blocked."}`.

A link looks like this:

```json
{
  "id": "6740c0e0a1b2c3d4e5f60718",
  "label": "Storenator 8081",
  "url": "http://storenator.bluelarma.com:8081/",
  "position": 0
}
```

| Method | Path | Body | Success |
| --- | --- | --- | --- |
| `GET` | `/api/links` | | `200` and the links in display order |
| `POST` | `/api/links` | `{ "url": "...", "label": "..." }` | `201`, a `Location` header, and the new link |
| `PATCH` | `/api/links/{id}` | `{ "label": "..." }` | `200` and the renamed link |
| `DELETE` | `/api/links/{id}` | | `204`. Remaining positions are packed from `0` |
| `PUT` | `/api/links/order` | `{ "ids": ["...", "..."] }` | `200` and the links in the new order |

`label` on create is optional. The reorder list must contain every current id exactly once.

Errors use `{"message":"..."}`.

| Status | When |
| --- | --- |
| `400` | The URL, label, order, or JSON body is not valid |
| `404` | The link id does not exist |
| `409` | That URL is already in the portal |
| `403` | The browser marked the change as cross-site |

## Testing

```bash
./gradlew test
```

Tests need the same MongoDB host. They use the database `homeportal-test`, configured in `src/test/resources/application.properties`, and clear that database before each API test. They do not write to `homeportal`.

## Project structure

```text
src/main/java/com/bluelarma/homeportal/   Application, API, and MongoDB mapping
src/main/resources/static/                 Portal page, styles, and script
src/main/resources/application.properties  Runtime configuration
src/test/java/                             API and URL tests
```

Build output, the IntelliJ project directory, and the old local database directory are gitignored.

## Contributing

1. Fork the repository and create a branch from `main`.
2. Make the change in the existing style.
3. Run `./gradlew test`.
4. Open a pull request that describes the behavior you changed.

## License

This repository does not include a license. All rights are reserved by the copyright holder.
