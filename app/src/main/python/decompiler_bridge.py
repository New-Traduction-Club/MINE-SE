import os
import sys
import traceback
from pathlib import Path
import unrpyc
from unrpyc import Context, get_ast
import decompiler

def decompile_file(input_path_str, output_path_str=None, overwrite=True, try_harder=False):
    """
    If output_path_str:
      - If it is a directory, writes to output_path_str / (input_filename.stem + ext)
      - If it is a full file path, writes to output_path_str
    If output_path_str is None, writes in-place next to input_path_str.
    """
    try:
        input_path = Path(input_path_str)
        if not input_path.exists():
            return {
                "success": False,
                "error": f"Input file not found: {input_path_str}",
                "output_path": None,
                "logs": []
            }
        
        if input_path.suffix == '.rpyc':
            ext = '.rpy'
        elif input_path.suffix == '.rpymc':
            ext = '.rpym'
        else:
            ext = '.rpy'

        if output_path_str:
            out_path = Path(output_path_str)
            if out_path.is_dir() or str(output_path_str).endswith(os.sep) or str(output_path_str).endswith('/'):
                out_path = out_path / (input_path.stem + ext)
        else:
            out_path = input_path.with_suffix(ext)

        out_path.parent.mkdir(parents=True, exist_ok=True)

        if not overwrite and out_path.exists():
            return {
                "success": True,
                "skipped": True,
                "output_path": str(out_path),
                "logs": [f"Skipped {input_path.name} (already exists)"]
            }

        context = Context()
        ast = get_ast(input_path, try_harder, context)
        with out_path.open('w', encoding='utf-8') as out_file:
            options = decompiler.Options(log=context.log_contents)
            decompiler.pprint(out_file, ast, options)

        return {
            "success": True,
            "skipped": False,
            "output_path": str(out_path),
            "logs": context.log_contents
        }
    except Exception as e:
        err_msg = str(e) or type(e).__name__
        tb = traceback.format_exc()
        return {
            "success": False,
            "error": f"{err_msg}\n{tb}",
            "output_path": None,
            "logs": []
        }

def find_rpyc_files(directory_path_str):
    try:
        root = Path(directory_path_str)
        if not root.exists() or not root.is_dir():
            return []
        
        found = []
        for p in root.rglob("*.rpyc"):
            if p.is_file():
                found.append(str(p.resolve()))
        for p in root.rglob("*.rpymc"):
            if p.is_file():
                found.append(str(p.resolve()))
        return sorted(found)
    except Exception:
        return []
