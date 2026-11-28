import importlib.util
import unittest
import tempfile
from pathlib import Path
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / "sync-docs.py"
SPEC = importlib.util.spec_from_file_location("sync_docs", SCRIPT)
sync_docs = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(sync_docs)


class DocumentationLinkConversionTest(unittest.TestCase):
    def test_example_tutorial_link_accounts_for_both_directory_mappings(self):
        source = sync_docs.RESOURCES_DIR / "examples/basic_topologies/README.md"
        self.assertEqual("[Buck](../../tutorials/dcdc/buck-converter.md)",
                         sync_docs.convert_links("[Buck](../tutorials/2xx_dcdc_converters/201_buck_converter/index.md)", source))

    def test_nested_getting_started_link_keeps_its_required_parent_levels(self):
        source = sync_docs.RESOURCES_DIR / "tutorials/1xx_getting_started/103_pwm_basics/README.md"
        self.assertEqual("[Buck](../../tutorials/dcdc/buck-converter.md)",
                         sync_docs.convert_links("[Buck](../../2xx_dcdc_converters/201_buck_converter/index.md)", source))

    def test_existing_directory_link_becomes_a_valid_markdown_file(self):
        source = sync_docs.RESOURCES_DIR / "examples/basic_topologies/README.md"
        self.assertEqual("[Buck](buck_converter/index.md)", sync_docs.convert_links("[Buck](buck_converter/)", source))

    def test_legacy_directory_link_uses_the_same_page_as_its_index(self):
        source = sync_docs.RESOURCES_DIR / "examples/basic_topologies/README.md"
        self.assertEqual("[Buck](../../tutorials/dcdc/buck-converter.md)",
                         sync_docs.convert_links("[Buck](../tutorials/2xx_dcdc_converters/201_buck_converter/)", source))

    def test_resource_root_link_with_missing_parent_maps_to_existing_page(self):
        source = sync_docs.RESOURCES_DIR / "tutorials/1xx_getting_started/README.md"
        self.assertEqual("[Examples](../examples/basic/index.md)",
                         sync_docs.convert_links("[Examples](../examples/basic_topologies/)", source))

    def test_missing_full_bridge_page_points_to_a_named_existing_example(self):
        source = sync_docs.RESOURCES_DIR / "examples/basic_topologies/README.md"
        self.assertEqual("[Dual Active Bridge](../power-supplies/dab.md)",
                         sync_docs.convert_links("[Full Bridge](full_bridge/index.md)", source))

    def test_external_links_and_fenced_examples_are_literal(self):
        source = sync_docs.RESOURCES_DIR / "examples/basic_topologies/README.md"
        content = "[External](https://example.com/2xx_dcdc_converters/index.md)\n```md\n[Literal](../../tutorials/example.md)\n```\n"
        self.assertEqual(content, sync_docs.convert_links(content, source))

    def test_article_sync_preserves_existing_destination_assets(self):
        # Keep the scratch directory for inspection; no automatic file deletion.
        root = Path(tempfile.mkdtemp(prefix="gecko-doc-sync-"))
        articles = root / "resources/articles"
        docs = root / "docs"
        source_assets = articles / "img"
        destination_assets = docs / "articles/img"
        source_assets.mkdir(parents=True)
        destination_assets.mkdir(parents=True)
        (source_assets / "new.txt").write_text("new asset")
        (destination_assets / "existing.txt").write_text("preserve this")
        with patch.multiple(sync_docs, PROJECT_ROOT=root, ARTICLES_DIR=articles, DOCS_DIR=docs):
            sync_docs.sync_articles()
            sync_docs.sync_articles()
        self.assertEqual("new asset", (destination_assets / "new.txt").read_text())
        self.assertEqual("preserve this", (destination_assets / "existing.txt").read_text())


if __name__ == "__main__":
    unittest.main()
