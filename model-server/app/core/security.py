from typing import Optional

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


def verify_internal_token(authorization: Optional[str] = Header(default=None)) -> None:
    settings = get_settings()
    if not settings.internal_token:
        return

    expected = f"Bearer {settings.internal_token}"
    if authorization != expected:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized",
        )
