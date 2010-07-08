import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

public abstract class CeylonTree {
  
  /**
   * Create a CeylonTree from an ANTLR tree.
   */
  public static CeylonTree build(Tree src) {
    Token token = ((CommonTree) src).getToken();
    if (token != null) {
      // ANTLR doesn't create a null top-level node when it
      // would only have one child.  We want one always, to
      // map to the compilation unit, so we create one where
      // necessary.
      Tree tmp = new CommonTree((Token) null);
      tmp.addChild(src);
      src = tmp;
    }
    return consume(src);
  }

  /**
   * Create a CeylonTree from an ANTLR tree.
   */
  private static CeylonTree consume(Tree src) {
    Class<? extends CeylonTree> klass = classFor(src);

    CeylonTree dst;
    try {
      dst = klass.newInstance();
    }
    catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    dst.children = dst.processChildren(src);

    return dst;
  }

  /**
   * Decide which class to use to represent this node
   */
  private static Class<? extends CeylonTree> classFor(Tree src) {
    Token token = ((CommonTree) src).getToken();
    if (token == null)
      return CompilationUnit.class;

    int type = token.getType();
    if (type == ceylonParser.TYPE_DECL) {
      // TYPE_DECL nodes are used to ensure that a type's
      // annotations are grouped together with the type
      // itself.  We rewrite them to bring the type (eg
      // CLASS_DECL) to the top, and put the annotations
      // as the child of the type.
      Tree firstChild = src.getChild(0);
      int childType = ((CommonTree) firstChild).getToken().getType();
      assert childType == ceylonParser.TYPE_DECL
          || childType == ceylonParser.CLASS_DECL
           : ceylonParser.tokenNames[childType];
      return classFor(firstChild);
    }
   
    Class<? extends CeylonTree> klass = classes.get(type);
    assert klass != null : type + ": " + ceylonParser.tokenNames[type];
    return klass;
  }

  /**
   * Mapping of ANTLR tokens to CeylonTree subclasses.
   */
  private static Map<Integer, Class<? extends CeylonTree>> classes;

  static {
    classes = new HashMap<Integer, Class<? extends CeylonTree>>();

    // NB CompilationUnit.class is handled in classFor(Tree)
    classes.put(ceylonParser.IMPORT_LIST,         ImportList.class);
    classes.put(ceylonParser.IMPORT_DECL,         ImportDeclaration.class);
    classes.put(ceylonParser.IMPORT_PATH,         ImportPath.class);
    classes.put(ceylonParser.IMPORT_WILDCARD,     ImportWildcard.class);
    classes.put(ceylonParser.ALIAS_DECL,          AliasDeclaration.class);
    classes.put(ceylonParser.CLASS_DECL,          ClassDeclaration.class);

    // XXX possibly rubbish node subclasses    
    
    classes.put(ceylonParser.ANNOTATION,          Annotation.class);
    classes.put(ceylonParser.ANNOTATION_LIST,     AnnotationList.class);
    classes.put(ceylonParser.ANNOTATION_NAME,     AnnotationName.class);
    classes.put(ceylonParser.ARG_LIST,            ArgumentList.class);
    classes.put(ceylonParser.CALL_EXPR,           CallExpression.class);
    classes.put(ceylonParser.DOT,                 Dot.class);
    classes.put(ceylonParser.EXPR,                Expression.class);
    classes.put(ceylonParser.LIDENTIFIER,         LIdentifier.class);
    classes.put(ceylonParser.MEMBER_NAME,         MemberName.class);
    classes.put(ceylonParser.STMT_LIST,           StatementList.class);
    classes.put(ceylonParser.SIMPLESTRINGLITERAL, SimpleStringLiteral.class);
    classes.put(ceylonParser.STRING_CST,          StringConstant.class);
    classes.put(ceylonParser.TYPE_NAME,           TypeName.class);
    classes.put(ceylonParser.UIDENTIFIER,         UIdentifier.class);
  }

  /**
   * This node's children.
   */
  public List<CeylonTree> children;

  /**
   * Initialize this node's children.  The default behaviour is to
   * recursively consume the tree, though this may be overridden or
   * extended by subclasses.
   */
  protected List<CeylonTree> processChildren(Tree src) {
    ListBuffer<CeylonTree> children = new ListBuffer<CeylonTree>();
    for (int i = 0; i < src.getChildCount(); i++)
      children.append(consume(src.getChild(i)));
    return children.toList();
  }

  /**
   * Base class for visitors.
   */
  public static class Visitor {
    public void visit(CompilationUnit that)     { visitDefault(that); }
    public void visit(ImportList that)          { visitDefault(that); }
    public void visit(ImportDeclaration that)   { visitDefault(that); }
    public void visit(ImportPath that)          { visitDefault(that); }
    public void visit(ImportWildcard that)      { visitDefault(that); }
    public void visit(AliasDeclaration that)    { visitDefault(that); }
    public void visit(ClassDeclaration that)    { visitDefault(that); }

    // XXX possibly rubbish node subclasses

    public void visit(Annotation that)          { visitDefault(that); }
    public void visit(AnnotationList that)      { visitDefault(that); }
    public void visit(AnnotationName that)      { visitDefault(that); }
    public void visit(ArgumentList that)        { visitDefault(that); }
    public void visit(CallExpression that)      { visitDefault(that); }
    public void visit(Dot that)                 { visitDefault(that); }
    public void visit(Expression that)          { visitDefault(that); }
    public void visit(LIdentifier that)         { visitDefault(that); }
    public void visit(MemberName that)          { visitDefault(that); }
    public void visit(StatementList that)       { visitDefault(that); }
    public void visit(SimpleStringLiteral that) { visitDefault(that); }
    public void visit(StringConstant that)      { visitDefault(that); }
    public void visit(TypeName that)            { visitDefault(that); }
    public void visit(UIdentifier that)         { visitDefault(that); }

    public void visitDefault(CeylonTree tree) {
      throw new RuntimeException();
    }
  }

  /**
   * Visit this tree with a given visitor.
   */
  public abstract void accept(Visitor v);

  /**
   * Convert a tree to a pretty-printed string
   */
  public String toString() {
    StringWriter s = new StringWriter();
    this.accept(new CeylonTreePrinter(s));
    return s.toString();
  }


  // Node subclasses

  /**
   * A compilation unit represents one source file.
   */
  public static class CompilationUnit extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A list of import declarations.
   */
  public static class ImportList extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An import declaration.
   */
  public static class ImportDeclaration extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An import path.
   */
  public static class ImportPath extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A wildcard at the end of an import path.
   */
  public static class ImportWildcard extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A type declaration.  This abstract class is the parent
   * class for items which may be wrapped in TYPE_DECL nodes
   * in the parse tree, and contains the functionality to
   * unwrap them.
   */
  public static abstract class TypeDeclaration extends CeylonTree {
    protected List<CeylonTree> processChildren(Tree src) {
      assert ((CommonTree) src).getToken().getType() == ceylonParser.TYPE_DECL;

      // Firstly, remove TYPE_DECL nodes that have exactly
      // one child that is also a TYPE_DECL node.
      if (src.getChildCount() == 1) {
        Tree child = src.getChild(0);
        if (((CommonTree) child).getToken().getType() == ceylonParser.TYPE_DECL)
          return processChildren(child);
      }

      // Secondly, rewrite:
      //   TYPE_DECL
      //     CLASS_DECL | INTERFACE_DECL | ALIAS_DECL | MEMBER_DECL
      //       children*
      //     ANNOTATION_LIST?
      // as:
      //     CLASS_DECL | INTERFACE_DECL | ALIAS_DECL | MEMBER_DECL
      //       children*
      //       ANNOTATION_LIST?
      assert src.getChildCount() == 1 || src.getChildCount() == 2;
      ListBuffer<CeylonTree> children = new ListBuffer<CeylonTree>();
      children.appendList(super.processChildren(src.getChild(0)));
      for (int i = 1; i < src.getChildCount(); i++)
        children.append(consume(src.getChild(i)));
      return children.toList();
    }
  }
  
  /**
   * An alias declaration.
   */
  public static class AliasDeclaration extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A class declaration.
   */
  public static class ClassDeclaration extends TypeDeclaration {
    public void accept(Visitor v) { v.visit(this); }
  }

  
  // XXX possibly rubbish node subclasses

  /**
   * An annotation.
   */
  public static class Annotation extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A list of annotations.
   */
  public static class AnnotationList extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An annotation name.
   */
  public static class AnnotationName extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A list of arguments.
   */
  public static class ArgumentList extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A call expression.
   */
  public static class CallExpression extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A dot.
   */
  public static class Dot extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An expression.
   */
  public static class Expression extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An lowercase identifier.
   */
  public static class LIdentifier extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A member name.
   */
  public static class MemberName extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A list of statements.
   */
  public static class StatementList extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A simple string literal.
   */
  public static class SimpleStringLiteral extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A string constant.
   */
  public static class StringConstant extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * A type name.
   */
  public static class TypeName extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }

  /**
   * An uppercase identifier.
   */
  public static class UIdentifier extends CeylonTree {
    public void accept(Visitor v) { v.visit(this); }
  }
}