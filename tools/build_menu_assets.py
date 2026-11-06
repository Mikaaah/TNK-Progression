"""Build Character and HUD from the supplied native CleanLook parts."""
from pathlib import Path
import sys
from cleanlook_assets import Assets,Canvas
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/tnkprogression'
a=Assets(sys.argv[1]);a.export(root,'tnkprogression')
c=Canvas(a,526,312);c.shell((14,114));c.panel(6,16,126,286,True);c.panel(140,42,374,260);c.panel(16,48,108,162,True);c.rect(20,52,100,154,'#20293F');c.save(root,'character')
c=Canvas(a,194,68);c.panel(0,0,194,68,True);c.save(root,'character_hud')
print('CLEANLOOK_CHARACTER_OK: source panels, no corner pins')
