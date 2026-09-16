import tempfile
import unittest
from pathlib import Path

from PIL import Image

import process_sequence


class SequenceProcessingTests(unittest.TestCase):
    def test_process_frame_preserves_dimensions_and_removes_low_alpha(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / "source.png"
            output = root / "output.webp"
            image = Image.new("RGBA", (8, 6), (80, 100, 120, 0))
            image.putpixel((3, 3), (255, 255, 255, 255))
            image.putpixel((4, 3), (0, 0, 0, 64))
            image.save(source, "PNG")

            process_sequence.process_frame(source, output, alpha_cutoff=192)

            with Image.open(output) as processed:
                processed.load()
                self.assertEqual(processed.size, (8, 6))
                self.assertEqual(processed.mode, "RGBA")
                self.assertEqual(processed.getpixel((3, 3))[3], 255)
                self.assertEqual(processed.getpixel((4, 3))[3], 0)


if __name__ == "__main__":
    unittest.main()
