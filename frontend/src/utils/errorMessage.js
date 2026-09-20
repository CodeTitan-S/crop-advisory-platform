const NETWORK_ERROR_MESSAGE =
  'Cannot reach server. The backend may be starting up (this takes ~1 min on free hosting), ' +
  'or CORS is blocking the request. Please wait and try again.';

/**
 * Turns an axios error into a user-facing message.
 *
 * @param {*} err the caught error
 * @param {string} fallback message used when the error carries no useful detail
 */
export default function getErrorMessage(err, fallback = 'Something went wrong') {
  if (err?.response) {
    // The server responded with an error status.
    return (
      err.response.data?.message ||
      err.response.data?.error ||
      `Server error: ${err.response.status}`
    );
  }
  if (err?.request) {
    // The request was made but no response arrived (CORS, network, backend down).
    return NETWORK_ERROR_MESSAGE;
  }
  return err?.message || fallback;
}
