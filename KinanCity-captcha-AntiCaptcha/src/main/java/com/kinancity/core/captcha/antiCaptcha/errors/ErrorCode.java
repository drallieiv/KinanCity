package com.kinancity.core.captcha.antiCaptcha.errors;

public enum ErrorCode {
	ERROR_KEY_DOES_NOT_EXIST // Account authorization key not found in the system
	, ERROR_NO_SLOT_AVAILABLE // No idle captcha workers are available at the moment, please try a bit later or try increasing your maximum bid here
	, ERROR_ZERO_CAPTCHA_FILESIZE // The size of the captcha you are uploading is less than 100 bytes.
	, ERROR_TOO_BIG_CAPTCHA_FILESIZE // The size of the captcha you are uploading is more than 500,000 bytes.
	, ERROR_ZERO_BALANCE // Account has zeo or negative balance
	, ERROR_IP_NOT_ALLOWED // Request with current account key is not allowed from your IP. Please refer to IP list section located here
	, ERROR_CAPTCHA_UNSOLVABLE // Captcha could not be solved by 5 different workers
	, ERROR_BAD_DUPLICATES // 100% recognition feature did not work due to lack of amount of guess attempts
	, ERROR_NO_SUCH_METHOD // Request to API made with method which does not exist
	, ERROR_IMAGE_TYPE_NOT_SUPPORTED // Could not determine captcha file type by its exif header or image type is not supported. The only allowed formats are JPG, GIF, PNG
	, ERROR_NO_SUCH_CAPCHA_ID // Captcha you are requesting does not exist in your current captchas list or has been expired. Captchas are removed from API after 5 minutes after upload.
	, ERROR_EMPTY_COMMENT // "comment" property is required for this request
	, ERROR_IP_BLOCKED // Your IP is blocked due to API inproper use. Check the reason at https://anti-captcha.com/panel/tools/ipsearch
	, ERROR_TASK_ABSENT // Task property is empty or not set in createTask method. Please refer to API v2 documentation.
	, ERROR_TASK_NOT_SUPPORTED // Task type is not supported or inproperly printed. Please check \"type\" parameter in task object.
	, ERROR_INCORRECT_SESSION_DATA // Some of the required values for successive user emulation are missing.
	, ERROR_PROXY_CONNECT_REFUSED // Could not connect to proxy related to the task, connection refused
	, ERROR_PROXY_CONNECT_TIMEOUT // Could not connect to proxy related to the task, connection timeout
	, ERROR_PROXY_READ_TIMEOUT // Connection to proxy for task has timed out
	, ERROR_PROXY_BANNED // Proxy IP is banned by target service
	, ERROR_PROXY_TRANSPARENT // Task denied at proxy checking state. Proxy must be non-transparent to hide our server IP.
	, ERROR_RECAPTCHA_TIMEOUT // Recaptcha task timeout, probably due to slow proxy server or Google server
	, ERROR_RECAPTCHA_INVALID_SITEKEY // Recaptcha server reported that site key is invalid
	, ERROR_RECAPTCHA_INVALID_DOMAIN // Recaptcha server reported that domain for this site key is invalid
	, ERROR_RECAPTCHA_OLD_BROWSER // Recaptcha server reported that browser user-agent is not compatible with their javascript
	, ERROR_RECAPTCHA_STOKEN_EXPIRED // Recaptcha server reported that stoken parameter has expired. Make your application grab it faster.
	, ERROR_PROXY_HAS_NO_IMAGE_SUPPORT // Proxy does not support transfer of image data from Google servers
	, ERROR_PROXY_INCOMPATIBLE_HTTP_VERSION // Proxy does not support long GET requests with length about 2000 bytes and does not support SSL connections
	, ERROR_FACTORY_SERVER_API_CONNECTION_FAILED // Could not connect to Factory Server API within 5 seconds
	, ERROR_FACTORY_SERVER_BAD_JSON // Incorrect Factory Server JSON response, something is broken
	, ERROR_FACTORY_SERVER_ERRORID_MISSING // Factory Server API did not send any errorId
	, ERROR_FACTORY_SERVER_ERRORID_NOT_ZERO // Factory Server API reported errorId != 0, check this error
	, ERROR_FACTORY_MISSING_PROPERTY // Some of the required property values are missing in Factory form specifications. Customer must send all required values.
	, ERROR_FACTORY_PROPERTY_INCORRECT_FORMAT // Expected other type of property value in Factory form structure. Customer must send specified value type.
	, ERROR_FACTORY_ACCESS_DENIED // Factory control belong to another account, check your account key.
	, ERROR_FACTORY_SERVER_OPERATION_FAILED // Factory Server general error code
	, ERROR_FACTORY_PLATFORM_OPERATION_FAILED // Factory Platform general error code.
	, ERROR_FACTORY_PROTOCOL_BROKEN // Factory task lifetime protocol broken during task workflow.
	, ERROR_FACTORY_TASK_NOT_FOUND // Task not found or not available for this operation
	, ERROR_FACTORY_IS_SANDBOXED // Factory is sandboxed, creating tasks is possible only by Factory owner. Switch it to production mode to make it available for other customers.
	, ERROR_PROXY_NOT_AUTHORISED // Proxy login and password are incorrect
}