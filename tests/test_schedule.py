import json
from pathlib import Path
import unittest

class ScheduleTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data=json.loads((Path(__file__).resolve().parents[1]/'app/src/main/assets/schedule.json').read_text())
        cls.days=cls.data['days']
    def test_contiguous_source_and_extension(self):
        self.assertEqual(self.data['mainDays'],120)
        self.assertEqual([d['day'] for d in self.days],list(range(1,127)))
        for d in self.days:
            self.assertEqual(d['week'],(d['day']-1)//7+1)
            self.assertTrue(d['summary'])
    def test_calendar_cells_not_pdf_reading_order(self):
        self.assertIn('Empresarial',self.days[28]['summary'])
        self.assertIn('Consumidor',self.days[53]['summary'])
        self.assertIn('OAB 40',self.days[85]['summary'])
        self.assertIn('Ética',self.days[119]['summary'])
        self.assertIn('Civil',self.days[124]['summary'])
    def test_detail_coverage_honest(self):
        self.assertTrue(all(d['details'] for d in self.days[:73]))
        self.assertTrue(all(not d['details'] for d in self.days[73:]))
        self.assertIn('TEORIA DA',self.days[0]['details'])
        self.assertNotIn('DIA 02:',self.days[0]['details'])
    def test_rest_and_mock_days(self):
        for n in [7,14,18,24,28,35,42,49,55,69,70,71,72,73,84]:
            self.assertEqual(self.days[n-1]['kind'],'rest')
        for n in [56,63,83,86,89,97,105,112,119,121]:
            self.assertEqual(self.days[n-1]['kind'],'mock')
        self.assertEqual(self.days[125]['kind'],'exam')
    def test_no_duplicate_extracted_print_layers(self):
        self.assertNotIn('PrincípiosPrincípios',self.days[3]['summary'])

if __name__=='__main__': unittest.main()
