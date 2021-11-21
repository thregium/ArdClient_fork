from __pbot.variables import Vars

class Script(Vars):
    def run(self):
        self.window = self.pbotutils().PBotWindow(self.ui, "Butcher", 110, 110, self.scriptid, True)
        self.start = self.window.addButton("skin", "Start", 100, 5, 85)
        body = self.pbotgobapi().findGobByNames(self.ui, 50.0, self.toJStringArray(["gfx/terobjs/barrel"]))
        self.pbotutils().sysMsg(self.ui, body.getResname())
        self.a = "4"


    def skin(self):
        body = self.pbotgobapi().findGobByNames(self.ui, 50.0, ["gfx/terobjs/barrel"])
        self.pbotutils().sysMsg(self.ui, body.getResname())