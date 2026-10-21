#!/usr/bin/env bash
# Captures the real server's answers into this folder, verbatim, for the
# parsing tests (task 03). Re-run after a server upgrade and `git diff` the
# result: that diff is the server's change log as far as we are concerned.
#
#   app/src/test/fixtures/capture.sh
#
# Asks for server, username and password. The password is hashed here exactly
# as the app does (sha256(username + sha256(password))) and never written down.
# Needs curl, shasum and jq (all on macOS).
#
# The session is ended with `goodbye` at the end, so the token that appears in
# handshake.json and in the songs' "url" fields is already dead when committed.
set -euo pipefail

dir="$(cd "$(dirname "$0")" && pwd)"

read -rp "Server (https://…): " server
server="${server%/}"
[[ "$server" == http://* || "$server" == https://* ]] || server="https://$server"
read -rp "Username: " user
read -rsp "Password: " password
echo

sha256() { printf '%s' "$1" | shasum -a 256 | cut -d' ' -f1; }
handshake_auth="$(sha256 "$user$(sha256 "$password")")"
unset password

# call <output-name> <auth> <action> [key=value ...] — saves the body verbatim.
call() {
    local name="$1" auth="$2" action="$3"
    shift 3
    local args=(--get --silent --show-error "$server/server/json.server.php"
        --data-urlencode "action=$action" --data-urlencode "auth=$auth")
    local kv
    for kv in "$@"; do args+=(--data-urlencode "$kv"); done
    curl "${args[@]}" > "$dir/$name.json"
    printf '  %-28s %7s bytes\n' "$name.json" "$(wc -c < "$dir/$name.json" | tr -d ' ')"
}

# first <file> <jq-path> — the first value at the path, or empty.
first() { jq -r "$2 // empty" "$dir/$1.json"; }

echo "Capturing into $dir"
call handshake "$handshake_auth" handshake version=6.0.0
token="$(first handshake .auth)"
if [[ -z "$token" ]]; then
    echo "Handshake failed:" >&2
    cat "$dir/handshake.json" >&2
    exit 1
fi

call albums "$token" albums offset=0 limit=5
album_id="$(first albums '.album[0].id')"
album_name="$(first albums '.album[0].name')"
artist_id="$(first albums '.album[0].artist.id')"
call album "$token" album "filter=$album_id"
call album_songs "$token" album_songs "filter=$album_id"
song_id="$(first album_songs '.song[0].id')"
song_title="$(first album_songs '.song[0].title')"

call artists "$token" artists offset=0 limit=5
call artist "$token" artist "filter=$artist_id"
call artist_albums "$token" artist_albums "filter=$artist_id"

call genres "$token" genres offset=0 limit=5
genre_id="$(first album '(.album // .) | (if type == "array" then .[0] else . end) | .genre[0].id')"
[[ -n "$genre_id" ]] || genre_id="$(first genres '.genre[0].id')"
call genre "$token" genre "filter=$genre_id"
call genre_albums "$token" genre_albums "filter=$genre_id"
# Real genre and playlist song lists run to thousands of rows; 20 parse the same.
call genre_songs "$token" genre_songs "filter=$genre_id" offset=0 limit=20

call playlists "$token" playlists offset=0 limit=5
playlist_id="$(first playlists '.playlist[0].id')"
call playlist "$token" playlist "filter=$playlist_id"
call playlist_songs "$token" playlist_songs "filter=$playlist_id" offset=0 limit=20

call song "$token" song "filter=$song_id"

call advanced_search_album_title "$token" advanced_search type=album operator=and \
    rule_1=title rule_1_operator=0 "rule_1_input=$album_name" offset=0 limit=50
call advanced_search_song_title "$token" advanced_search type=song operator=and \
    rule_1=title rule_1_operator=0 "rule_1_input=$song_title" offset=0 limit=50

# A token the server has never issued: the Session Expired envelope.
call error_session_expired 0000000000000000000000000000000000000000000000000000000000000000 albums offset=0 limit=1

curl --get --silent --output /dev/null "$server/server/json.server.php" \
    --data-urlencode action=goodbye --data-urlencode "auth=$token"
echo "Done; session ended. Review with: git diff --stat -- app/src/test/fixtures"
