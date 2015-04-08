# PLUGIN_NAME.*

> --------------------- ------------------------------------------------------------------------------------------
> __Type__              [library][api.type.library]
> __Revision__          [REVISION_LABEL](REVISION_URL)
> __Keywords__          PLUGIN_NAME
> __Sample code__       
> __See also__          
> __Availability__      Starter, Pro, Enterprise
> --------------------- ------------------------------------------------------------------------------------------

## Overview

The PLUGIN_NAME plugin enables X.

## Sign Up

To use the SERVICE_NAME service, please [sign up](CORONA_REFERRAL_URL) for an account.

## Platforms

* Android: Yes
* iOS: Yes
* Mac: No
* Win: No
* Kindle: No
* NOOK: No

## Syntax

	local PLUGIN_NAME = require "plugin.PLUGIN_NAME"

## Functions

#### [PLUGIN_NAME.FUNCTION()][plugin.PLUGIN_NAME.FUNCTION]

#### [PLUGIN_NAME.PROPERTY][plugin.PLUGIN_NAME.PROPERTY]

## Project Settings

### SDK

When you build using the Corona Simulator, the server automatically takes care of integrating the plugin into your project. 

All you need to do is add an entry into a `plugins` table of your `build.settings`. The following is an example of a minimal `build.settings` file:

``````
settings =
{
	plugins =
	{
		-- key is the name passed to Lua's 'require()'
		["plugin.PLUGIN_NAME"] =
		{
			-- required
			publisherId = "REVERSE_PUBLISHER_URL",
		},
	},		
}
``````

### Enterprise

TBD

## Sample Code

You can access sample code [here](SAMPLE_CODE_URL).

## Support

More support is available from the PUBLISHER_NAME team:

* [E-mail](mailto://PUBLISHER_CONTACT@PUBLISHER_URL)
* [Forum](http://forum.coronalabs.com/plugin/PLUGIN_NAME)
* [Plugin Publisher](http://PUBLISHER_URL)
