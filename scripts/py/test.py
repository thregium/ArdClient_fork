from __pbot.variables import Vars

class Script(Vars):
    def run(self):
        self.window = self.pbotutils().PBotWindow(self.ui, "Butcher", 110, 110, self.scriptid, True)
        self.start = self.window.addButton("skin", "Start", 100, 5, 85)
        self.a = "4"


    def skin(self):
        self.pbotutils().sysMsg(self.ui, self.a)
