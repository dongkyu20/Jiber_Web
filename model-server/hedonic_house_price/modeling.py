class TrainedModel:
    """Minimal runtime shape needed by uploaded training artifacts."""

    def predict(self, rows):
        return self.pipeline.predict(rows)


class TargetEncodingMap:
    """Pickle compatibility container for training-time target encodings."""
