// Fix "document is not defined" error when running inside a WebWorker context.
// Webpack's jsonp chunk loading runtime emits `document.baseURI` directly
// (not guarded via __webpack_require__.g), which crashes in Worker scope.
// This plugin patches the emitted source to use self.location.href instead.
const { sources, Compilation } = require("webpack");

class WorkerDocumentPatchPlugin {
    apply(compiler) {
        compiler.hooks.compilation.tap("WorkerDocumentPatchPlugin", (compilation) => {
            compilation.hooks.processAssets.tap(
                { name: "WorkerDocumentPatchPlugin", stage: Compilation.PROCESS_ASSETS_STAGE_OPTIMIZE_INLINE },
                (assets) => {
                    for (const [name, asset] of Object.entries(assets)) {
                        if (!name.endsWith(".js")) continue;
                        const original = asset.source();
                        const patched = original.replace(
                            /\bdocument\.baseURI\b/g,
                            "(typeof document !== 'undefined' ? document.baseURI : self.location.href)"
                        );
                        if (patched !== original) {
                            compilation.updateAsset(name, new sources.RawSource(patched));
                        }
                    }
                }
            );
        });
    }
}

config.plugins = config.plugins || [];
config.plugins.push(new WorkerDocumentPatchPlugin());
