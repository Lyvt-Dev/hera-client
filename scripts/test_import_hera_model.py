import unittest
from import_hera_model import expression,sample
class ImportTests(unittest.TestCase):
 def test_molang_uses_degrees_and_animation_time(self):
  self.assertAlmostEqual(expression('math.sin(query.anim_time*360)*42',.25),42)
  self.assertEqual(expression('-2.5*math.clamp(math.cos(query.anim_time*60)*1.6,-1,1)',0),-2.5)
 def test_no_arbitrary_code(self):
  for source in ["__import__('os').system('whoami')",'query.other_time','t.__class__','[t]']:
   with self.assertRaises(ValueError):expression(source,0)
 def test_linear_and_bezier_curves(self):
  keys=[{'time':0,'data_points':[{'x':0}], 'interpolation':'linear'},{'time':1,'data_points':[{'x':10}]}]
  self.assertEqual(sample(keys,.25,0),2.5)
  keys[0].update(interpolation='bezier',bezier_right_time=[1/3,0,0],bezier_right_value=[0,0,0])
  keys[1].update(bezier_left_time=[-1/3,0,0],bezier_left_value=[0,0,0])
  self.assertAlmostEqual(sample(keys,.25,0),1.5625,places=4)
if __name__=='__main__':unittest.main()
