# Tasks

## 1. Navigation and page skeletons

- [x] 1.1 Add the seven nav entries to `mkdocs.yml` (placement per design D2) and create stub `*.en.md`/`*.ru.md` pairs for all new pages; verify local site build (`mkdocs build`) succeeds with all entries resolving and `python docs/check-parity.py` passes

## 2. Deep-dive topic pages (EN + RU each)

- [x] 2.1 Write the Items & controls page (tableview/columns/cellFormat/edit model, listview, combobox with `bindSelected` and autocompletion, treeview, DataGrid) per design D1 sources; verify parity check passes and every snippet compiles in a scratch test source against the current API
- [x] 2.2 Write the Charts and Dialogs & windows pages; verify parity check passes and snippets compile as written
- [x] 2.3 Write the Config & preferences and i18n & messages pages; verify parity check passes and snippets compile as written
- [x] 2.4 Write the Diagnostics page (layout debugger, event log, error handling); verify parity check passes and snippets compile as written

## 3. Testing guide

- [x] 3.1 Write the Testing guide (TestFX consumer setup for Maven and Gradle, headless Monocle execution, lifecycle patterns, link to the repo's headless profile as live reference) per design D5; verify parity check passes and the guide's dependency/property snippets match the versions and flags actually used by the repository build

## 4. Final verification

- [x] 4.1 Run the full local site build and re-run `docs/check-parity.py`; verify all seven new pages render in both languages, nav placement matches design D2, no dead links or links to removed APIs, and the site builds from a clean checkout state as CI would

## 5. Example images (added on request after 4.1)

- [x] 5.1 Generate real UI screenshots for the representative snippets by rendering the examples headless (Monocle) with the project's own test infrastructure into `docs/images/`: tableview, listview, combobox, treeview, datagrid (items-controls); pie, line, multiseries line (charts); alert, dialog form, internal window (dialogs-windows); settings form (config-preferences); messages view (i18n)
- [x] 5.2 Embed the images in both language versions of the affected pages with alt text; verify `mkdocs build --strict` resolves every image link and the parity check still passes
