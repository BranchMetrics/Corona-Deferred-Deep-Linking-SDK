local Library = require "CoronaLibrary"

-- Create library
local lib = Library:new{ name='branch', publisherId='io.branch' }

lib.init = function()
	native.showAlert( 'Notice!', 'Branch doesn\'t work on the Corona Simulator!', { 'OK' } )
end

lib.call = function()
	native.showAlert( 'Notice!', 'Branch doesn\'t work on the Corona Simulator!', { 'OK' } )
end

-------------------------------------------------------------------------------
-- END
-------------------------------------------------------------------------------

-- Return an instance
return lib
