import sys
import os
import shlex
import traceback
import runpy

class StreamInterceptor:
    def __init__(self, callback):
        self.callback = callback

    def write(self, s):
        if not s:
            return
        try:
            text = s.decode('utf-8', errors='replace') if isinstance(s, bytes) else str(s)
            if hasattr(self.callback, "onOutput"):
                self.callback.onOutput(text)
            elif callable(self.callback):
                self.callback(text)
        except Exception:
            pass

    def flush(self):
        pass

    def isatty(self):
        return True

class StdinInterceptor:
    def __init__(self, callback):
        self.callback = callback

    def readline(self, size=-1):
        try:
            if hasattr(self.callback, "onInputRequest"):
                res = self.callback.onInputRequest()
                if res is not None:
                    s = str(res)
                    if not s.endswith("\n"):
                        s += "\n"
                    return s
            elif callable(self.callback):
                res = self.callback()
                if res is not None:
                    s = str(res)
                    if not s.endswith("\n"):
                        s += "\n"
                    return s
        except Exception:
            pass
        return "\n"

    def read(self, size=-1):
        return self.readline(size)

    def readlines(self, hint=-1):
        lines = []
        line = self.readline()
        if line and line != "\n":
            lines.append(line)
        return lines

    def isatty(self):
        return True

def run_script(script_path_str, args_str="", callback=None):
    script_path = os.path.abspath(script_path_str)
    if not os.path.exists(script_path):
        err = f"Script not found: {script_path_str}"
        if callback:
            try:
                if hasattr(callback, "onOutput"):
                    callback.onOutput(err + "\n")
                elif callable(callback):
                    callback(err + "\n")
            except Exception:
                pass
        return {
            "success": False,
            "exit_code": -1,
            "error": err
        }

    script_dir = os.path.dirname(script_path)

    try:
        parsed_args = shlex.split(args_str or "")
    except Exception:
        parsed_args = (args_str or "").split()

    old_stdout = sys.stdout
    old_stderr = sys.stderr
    old_stdin = sys.stdin
    old_argv = list(sys.argv)
    old_cwd = os.getcwd()

    interceptor = StreamInterceptor(callback) if callback else None
    stdin_interceptor = StdinInterceptor(callback) if callback else None

    path_added = False
    if script_dir not in sys.path:
        sys.path.insert(0, script_dir)
        path_added = True

    try:
        if interceptor:
            sys.stdout = interceptor
            sys.stderr = interceptor
        if stdin_interceptor:
            sys.stdin = stdin_interceptor

        sys.argv = [script_path] + parsed_args
        os.chdir(script_dir)

        runpy.run_path(script_path, run_name="__main__")

        return {
            "success": True,
            "exit_code": 0,
            "error": None
        }
    except SystemExit as se:
        code = se.code
        exit_code = 0 if code is None else (code if isinstance(code, int) else 1)
        if exit_code != 0 and interceptor:
            interceptor.write(f"\n[Process exited with code {exit_code}]\n")
        return {
            "success": (exit_code == 0),
            "exit_code": exit_code,
            "error": None if exit_code == 0 else f"SystemExit({exit_code})"
        }
    except Exception as e:
        tb = traceback.format_exc()
        if interceptor:
            interceptor.write(f"\nTraceback (most recent call last):\n{tb}\n")
        return {
            "success": False,
            "exit_code": 1,
            "error": f"{type(e).__name__}: {str(e)}\n{tb}"
        }
    finally:
        sys.stdout = old_stdout
        sys.stderr = old_stderr
        sys.stdin = old_stdin
        sys.argv = old_argv
        try:
            os.chdir(old_cwd)
        except Exception:
            pass
        if path_added:
            try:
                sys.path.remove(script_dir)
            except Exception:
                pass
