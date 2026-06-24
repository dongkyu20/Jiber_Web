class HistGradientBoostingPipeline:
    """Delegates inference to the persisted sklearn Pipeline."""

    def predict(self, rows):
        return self.estimator.predict(rows)
