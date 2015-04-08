# Corona Plugin Submission Template

This is a template for plugin submission.

Please read the [Plugin Submission Guidelines](http://docs.coronalabs.com/daily/native/plugin/submission.html) for more information.

This template provides stubs for two of the top level directories that are needed in your plugin submission:

* `metadata.json` You should modify this with information about your company and plugin
* `docs/` 
    + `PLUGIN_NAME/` This folder should correspond to the name of , e.g. 'openudid'
        - `index.markdown` This is the top level page for your plugin's documentation
        - `FUNCTION.markdown` This is a stub for a library function offered your plugin
        - `PROPERTY.markdown` This is a stub for a library property offered by your plugin
* `plugins/`
    + `VERSION/`
        - `android/`
            - `metadata.lua` This is a stub for the metadata describing the binary
        - `iphone/`
            - `metadata.lua` This is a stub for the metadata describing the binary
        - `iphone-sim/`
            - `metadata.lua` This is a stub for the metadata describing the binary
        - `mac-sim/`
            - `plugin_PLUGIN_NAME.lua` This is a stub Lua file to be used by the Corona Simulator
        - `win32-sim/`
            - `plugin_PLUGIN_NAME.lua` This is a stub Lua file to be used by the Corona Simulator
* `samples/`
    + `build.settings` You should modify this so that the build server knows what plugins are needed. See instructions inside the file
    + `main.lua` You should modify this to demonstrate how to use the plugin.

The complete directory structure is explained in the [Plugin Submission Guidelines](http://docs.coronalabs.com/daily/native/plugin/submission.html)

## Replacing strings in ALL CAPS

In each file there are strings in ALL CAPS that should be replaced with information specific to your plugin. You should 'grep' for the following strings and replace them appropriately:

* `PLUGIN_NAME` This should be the name of the plugin. 
    + You should preserve any prefix such as `plugin.` or `plugin_`. 
    + Note the trailing '.' and '_', respectively.
    + Don't forget to rename any file __and__ directory with `PLUGIN_NAME` in it, e.g. `plugin_PLUGIN_NAME.lua` => `plugin_openudid.lua`.
* `VERSION`
    + This is a directory
    + You should rename this to the daily build version of Corona in which the plugin is available, e.g. 2013.1076.
* `PUBLISHER_CONTACT` The e-mail of the main contact person for support.
* `PUBLISHER_NAME` The brand name of the publisher.
* `PUBLISHER_URL` The url of the publisher
* `REVERSE_PUBLISHER_URL` The reverse domain that uniquely identifies the publisher, e.g. `com.mycompany`.
* `SERVICE_NAME` The name of the service provided by the publisher (if applicable)
* `CORONA_REFERRAL_URL` The referral link if the service requires a separate account registration.
* `SAMPLE_CODE_URL` The url to a Lua-based sample code for Corona developers.

### Exceptions (Do not replace)

Do not replace the following strings:

* `REVISION_LABEL`
* `REVISION_URL`

These are automatically updated by our documentation system.

## Markdown guidelines

Our documentation system is based on markdown. In particular, the [Pandoc markdown](http://johnmacfarlane.net/pandoc/README.html) flavor of markdown.

The documentation system converts the markdown into the HTML that you see on [Corona's documentation site](http://docs.coronalabs.com).

### Reference links

You can add links to any document in http://docs.coronalabs.com by using markdown-style reference links.

The reference links follow a simple convention. You essentially treat directories as namespaces. In other words, you look at the path where the markdown page exists and replace the directory delimiters ('/') with a period ('.'), removing the base url from the path. Here are some examples:

* `[Main Plugin Documentation page][plugin]`
* `[openudid documentation page][plugin.openudid]`
* `[display library page under SDK APIs][api.library.display]`

Our documentation system will insert definitions for reference links to any page that you use and translating the above link convention into actual page links for you. 

You should be able to refer to other markdown pages you supply or to existing documentation.

