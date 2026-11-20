from unittest.mock import patch

from django.test import TestCase


class CoreEndpointsTest(TestCase):
    def test_spa_shell_serves_index(self):
        res = self.client.get("/")
        self.assertEqual(res.status_code, 200)
        self.assertContains(res, "Lyra Melofy")

    def test_search_requires_query(self):
        res = self.client.get("/api/search")
        self.assertEqual(res.status_code, 400)

    @patch("core.youtube.search")
    def test_search_returns_songs(self, mock_search):
        mock_search.return_value = [
            {"id": "4D7u5KF7SP8", "title": "Get Lucky", "artist": "Daft Punk", "thumbnail": "https://example.com/1.jpg"}
        ]
        res = self.client.get("/api/search?q=daft+punk")
        self.assertEqual(res.status_code, 200)
        payload = res.json()
        self.assertEqual(payload["query"], "daft punk")
        self.assertEqual(len(payload["results"]), 1)
        self.assertEqual(payload["results"][0]["id"], "4D7u5KF7SP8")

    @patch("core.youtube.random_picks")
    def test_random_returns_seed_and_songs(self, mock_random):
        mock_random.return_value = (
            "pop hits",
            [{"id": "abc123", "title": "Sample", "artist": "Artist", "thumbnail": ""}],
        )
        res = self.client.get("/api/random")
        self.assertEqual(res.status_code, 200)
        payload = res.json()
        self.assertEqual(payload["seed"], "pop hits")
        self.assertEqual(len(payload["results"]), 1)
