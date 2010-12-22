import os
import sys

class Wrapper:
    def __init__(self):
        self.basedir = os.path.dirname(
            os.path.dirname(os.path.realpath(sys.argv[0])))
        self.boot_cp = []
        self.wrap_cp = []
        self.user_cp = []
        self.args = []
        tmp = sys.argv[1:]
        while tmp:
            arg = tmp.pop(0)
            for cp in ("cp", "classpath"):
                cp = "-" + cp
                if arg == cp:
                    self.user_cp = [tmp.pop(0)]
                    break
                cp += "="
                if arg.startswith(cp):
                    self.user_cp = [arg[len(cp):]]
                    break
            else:
                self.args.append(arg)

    def addJar(self, *parts):
        self.wrap_cp.append(os.path.join(self.basedir, *parts))

    def run(self, mainclass):
        args = ["java", "-ea"]
        if self.boot_cp:
            args.append(":".join(["-Xbootclasspath/p"] + self.boot_cp))
        args.append("-cp")
        args.append(":".join(self.wrap_cp + self.user_cp))
        args.append(mainclass)
        args.extend(self.args)
        os.execvp(args[0], args)