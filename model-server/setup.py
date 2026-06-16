from setuptools import find_packages, setup


setup(
    name="jiber-model-server",
    version="0.1.0",
    description="Jiber Phase 1 FastAPI model-server skeleton",
    packages=find_packages(include=["app", "app.*"]),
    python_requires=">=3.9",
)
