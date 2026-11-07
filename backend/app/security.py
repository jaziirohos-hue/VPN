import base64
import binascii
import hashlib
import hmac
import secrets


def new_token() -> str:
    return secrets.token_urlsafe(32)


def hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


def tokens_equal(a: str, b: str) -> bool:
    return hmac.compare_digest(a.encode(), b.encode())


def is_wireguard_key(value: str) -> bool:
    """A WireGuard key is 32 bytes, base64 encoded (44 characters)."""
    if len(value) != 44:
        return False
    try:
        return len(base64.b64decode(value, validate=True)) == 32
    except (binascii.Error, ValueError):
        return False
