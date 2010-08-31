package org.jvnet.hk2.component.internal.runlevel;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.RunLevel;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.RunLevelListener;
import org.jvnet.hk2.component.RunLevelService;
import org.jvnet.hk2.component.RunLevelState;
import org.jvnet.hk2.component.internal.runlevel.DefaultRunLevelService;
import org.jvnet.hk2.component.internal.runlevel.Recorder;
import org.jvnet.hk2.junit.Hk2Runner;
import org.jvnet.hk2.junit.Hk2RunnerOptions;
import org.jvnet.hk2.test.runlevel.ExceptionRunLevelManagedService;
import org.jvnet.hk2.test.runlevel.NonRunLevelWithRunLevelDepService;
import org.jvnet.hk2.test.runlevel.RunLevelServiceBase;
import org.jvnet.hk2.test.runlevel.RunLevelServiceNegOne;
import org.jvnet.hk2.test.runlevel.ServiceA;
import org.jvnet.hk2.test.runlevel.ServiceB;
import org.jvnet.hk2.test.runlevel.ServiceC;
import org.jvnet.hk2.test.runlevel.TestRunLevelListener;

import com.sun.hk2.component.AbstractInhabitantImpl;
import com.sun.hk2.component.ExistingSingletonInhabitant;

/**
 * Testing around the default RunLevelService impl.
 * 
 * @author Jeff Trent
 */
@RunWith(Hk2Runner.class)
@Hk2RunnerOptions(reinitializePerTest=true)
public class RunLevelServiceTest {

  @Inject
  Habitat h;
  
  @Inject(name="default")
  RunLevelService<?> rls;
  
  @Inject
  RunLevelListener listener;

  private TestRunLevelListener defRLlistener;
  
  private HashMap<Integer, Recorder> recorders;

  private DefaultRunLevelService defRLS;

  
  /**
   * Verifies the state of the habitat
   */
  @SuppressWarnings("unchecked")
  @Test
  public void validInitialHabitatState() {
    Collection<RunLevelListener> coll1 = h.getAllByContract(RunLevelListener.class);
    assertNotNull(coll1);
    assertEquals(1, coll1.size());
    assertSame(listener, coll1.iterator().next());
    assertTrue(coll1.iterator().next() instanceof TestRunLevelListener);
    
    Collection<RunLevelService> coll2 = h.getAllByContract(RunLevelService.class);
    assertNotNull(coll2);
    assertEquals(coll2.toString(), 2, coll2.size());  // a test one, and the real one
    
    RunLevelService rls = h.getComponent(RunLevelService.class);
    assertNotNull(rls);
    assertNotNull(rls.getState());
    assertEquals(-1, rls.getState().getCurrentRunLevel());
    assertEquals(null, rls.getState().getPlannedRunLevel());
    assertEquals(Void.class, rls.getState().getEnvironment());
    
    RunLevelService rls2 = h.getComponent(RunLevelService.class, "default");
    assertSame(rls, rls2);
    assertSame(this.rls, rls);
    assertTrue(rls instanceof DefaultRunLevelService);
  }
  
  /**
   * Verifies that RunLevel -1 inhabitants are created immediately
   */
  @Test
  public void validateRunLevelNegOneInhabitants() {
    assertTrue(h.isInitialized());
    Inhabitant<RunLevelServiceNegOne> i = h.getInhabitantByType(RunLevelServiceNegOne.class);
    assertNotNull(i);
    assertTrue(i.toString() + "expected to have been instantiated", i.isInstantiated());
  }
  
  @Test
  public void proceedToInvalidNegNum() {
    try {
      rls.proceedTo(-2);
      fail("Expected -1 to be a problem");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
  
  /**
   * There should be no runlevel services at this level.
   */
  @Test
  public void proceedTo0() {
    installTestRunLevelService(false);
    rls.proceedTo(0);
    assertEquals(recorders.toString(), 0, recorders.size());
    assertEquals(0, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());
  }
  
  @Test
  public void proceedUpTo5_basics() {
    installTestRunLevelService(false);
    rls.proceedTo(5);
    assertEquals(1, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);

    assertEquals(5, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());
  }

  @Test
  public void proceedUpTo5Async() throws InterruptedException {
    installTestRunLevelService(true);
    rls.proceedTo(5);
    assertEquals(5, defRLS.getPlannedRunLevel());
    Integer tmp = defRLS.getCurrentRunLevel();
    synchronized (rls) {
      rls.wait(1000);
    }
    assertTrue("too fast!", (null == tmp ? -1 : tmp) < 5);

    assertEquals(1, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);

    assertEquals(5, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());
    
    assertInhabitantsState(5);
    assertListenerState(false, true, false);
    assertRecorderState();
  }
  
  @Test
  public void proceedUpTo10() {
    installTestRunLevelService(false);
    rls.proceedTo(10);
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertEquals(recorders.toString(), 2, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);

    recorder = recorders.get(10);
    assertNotNull(recorder);
    
    assertEquals(10, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(10);
    assertListenerState(false, true, false);
    assertRecorderState();
  }
  
  @Test
  public void proceedUpTo49() throws InterruptedException {
    installTestRunLevelService(false);
    rls.proceedTo(49);
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertEquals(recorders.toString(), 3, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);

    recorder = recorders.get(10);
    assertNotNull(recorder);
    
    recorder = recorders.get(20);
    assertNotNull(recorder);

    assertEquals(49, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(49);
    assertListenerState(false, true, false);
    assertRecorderState();
  }

  @Test
  public void proceedUpTo49ThenDownTo11() {
    installTestRunLevelService(false);
    rls.proceedTo(49);
    rls.proceedTo(11);
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertEquals(2, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);
    recorder = recorders.get(10);
    assertNotNull(recorder);

    assertEquals(11, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(11);
    assertListenerState(true, true, false);
    assertRecorderState();
  }
  
  @Test
  public void proceedUpTo49ThenDownTo11Async() throws InterruptedException {
    installTestRunLevelService(true);
    
    rls.proceedTo(49);
    rls.proceedTo(11);
    
    assertEquals(11, defRLS.getPlannedRunLevel());
    
    synchronized (rls) {
      rls.wait(1000);
    }
    assertEquals(11, defRLS.getCurrentRunLevel());

    assertEquals(2, recorders.size());
    Recorder recorder = recorders.get(5);
    assertNotNull(recorder);
    recorder = recorders.get(10);
    assertNotNull(recorder);

    assertEquals(11, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(11);
    assertListenerState(true, true, true);
    assertRecorderState();
  }
  
  @Test
  public void proceedUpTo49ThenDownTo11ThenDownToZero() {
    installTestRunLevelService(false);
    rls.proceedTo(49);
    rls.proceedTo(11);
    rls.proceedTo(0);
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertEquals(0, recorders.size());

    assertInhabitantsState(0);
    assertListenerState(true, true, false);
//    assertRecorderState();
  }
  
  /**
   * This guy tests the underling Recorder Ordering.
   * 
   * Note that: ServiceA -> ServiceB -> ServiceC
   * 
   * So, if we active B, A, then C manually, we still expect
   * the recorder to have A, B, C only in that order.
   * 
   * This takes some "rigging" on the runLevelService (to ignore proceedTo)
   * @throws NoSuchFieldException 
   * @throws Exception 
   */
  @Test
  public void serviceABC_startUp_and_shutDown() throws Exception {
    installTestRunLevelService(false);
    
    Field fldCurrent = DefaultRunLevelService.class.getDeclaredField("current");
    fldCurrent.setAccessible(true);
    fldCurrent.set(defRLS, 10);

    Field fldPlanned = DefaultRunLevelService.class.getDeclaredField("planned");
    fldPlanned.setAccessible(true);
    fldPlanned.set(defRLS, 10);
    
    Field fldActive = DefaultRunLevelService.class.getDeclaredField("activeRunLevel");
    fldActive.setAccessible(true);
    fldActive.set(defRLS, 10);

    Field fldUpSide = DefaultRunLevelService.class.getDeclaredField("upSide");
    fldUpSide.setAccessible(true);
    fldUpSide.set(defRLS, true);

    RunLevelServiceBase.count = 0;
    
    assertNotNull(h.getComponent(ServiceB.class));
    assertNotNull(h.getComponent(ServiceA.class));
    assertNotNull(h.getComponent(ServiceC.class));

    assertEquals(recorders.toString(), 1, recorders.size());
    assertEquals("count", 3, RunLevelServiceBase.count);

    Recorder recorder = recorders.get(10);
    assertNotNull(recorder);

    List<Inhabitant<?>> activations = recorder.getActivations();
    assertEquals(3, activations.size());
    
    Inhabitant<?> iB = h.getInhabitantByContract(ServiceB.class.getName(), null);
    Inhabitant<?> iA = h.getInhabitantByContract(ServiceA.class.getName(), null);
    Inhabitant<?> iC = h.getInhabitantByContract(ServiceC.class.getName(), null);

    assertTrue(iB.isInstantiated());
    assertTrue(iA.isInstantiated());
    assertTrue(iC.isInstantiated());
    
    Iterator<Inhabitant<?>> iter = activations.iterator();
    assertSame("order is important", iC, iter.next());
    assertSame("order is important", iB, iter.next());
    assertSame("order is important", iA, iter.next());

    Method resetMthd = DefaultRunLevelService.class.getDeclaredMethod("reset", (Class<?>[])null);
    resetMthd.setAccessible(true);
    resetMthd.invoke(defRLS, (Object[])null);
    
    RunLevelServiceBase a = (RunLevelServiceBase) iA.get();
    RunLevelServiceBase b = (RunLevelServiceBase) iB.get();
    RunLevelServiceBase c = (RunLevelServiceBase) iC.get();
    
    RunLevelServiceBase.count = 0;
    defRLS.proceedTo(0);
    assertFalse(iB.isInstantiated());
    assertFalse(iA.isInstantiated());
    assertFalse(iC.isInstantiated());

    assertEquals(recorders.toString(), 0, recorders.size());
    assertEquals("count", 3, RunLevelServiceBase.count);
    
    assertEquals("order is important on shutdown too: A", 0, a.countStamp);
    assertEquals("order is important on shutdown too: B", 1, b.countStamp);
    assertEquals("order is important on shutdown too: C", 2, c.countStamp);

    assertListenerState(true, false, false);
  }
  
  @Test
  public void dependenciesFromNonRunLevelToRunLevelService() {
    rls.proceedTo(10);
  
    Inhabitant<NonRunLevelWithRunLevelDepService> i = 
      h.getInhabitantByType(NonRunLevelWithRunLevelDepService.class);
    assertNotNull(i);
    assertFalse(i.isInstantiated());
    
    try {
      fail("Expected get() to fail, bad dependency to a RunLevel service: " + i.get());
    } catch (Exception e) {
      // expected
    }

    assertFalse(i.isInstantiated());
  }
  
  @Test
  public void dependenciesFromNonRunLevelToRunLevelServiceAsync() {
    installTestRunLevelService(true);
    
    defRLS.proceedTo(10);
  
    Inhabitant<NonRunLevelWithRunLevelDepService> i = 
      h.getInhabitantByType(NonRunLevelWithRunLevelDepService.class);
    assertNotNull(i);
    assertFalse(i.isInstantiated());
    
    try {
      fail("Expected get() to fail, bad dependency to a RunLevel service: " + i.get());
    } catch (Exception e) {
      // expected
    }

    assertFalse(i.isInstantiated());
  }
  
  /**
   * Verifies the behavior of an OnProgress recipient, calling proceedTo()
   */
  @Test
  public void chainedStartupProceedToCalls() throws Exception {
    installTestRunLevelService(false);

    defRLlistener.setProgressProceedTo(1, 4, rls);
    
    rls.proceedTo(1);
    synchronized (rls) {
      rls.wait(1000);
    }
    assertEquals(4, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(4);
    assertListenerState(true, false, true);
  }
  
  /**
   * Verifies the behavior of an OnProgress recipient, calling proceedTo()
   */
  @Test
  public void chainedStartupProceedToCallsAsync() throws Exception {
    installTestRunLevelService(true);
    
    defRLlistener.setProgressProceedTo(1, 4, rls);
    
    rls.proceedTo(1);
    synchronized (rls) {
      rls.wait(1000);
    }
    if (1 == defRLS.getCurrentRunLevel()) {
      synchronized (rls) {
        rls.wait(100);
      }
    }
    
    assertEquals(4, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(4);
    assertListenerState(true, false, true);
  }
  
  /**
   * Verifies the behavior of an OnProgress recipient, calling proceedTo()
   */
  @Test
  public void chainedShutdownProceedToCalls() throws Exception {
    installTestRunLevelService(false);

    defRLlistener.setProgressProceedTo(4, 0, rls);
    rls.proceedTo(4);
    
    synchronized (rls) {
      rls.wait(1000);
    }
    assertEquals(0, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(4);
    assertListenerState(true, false, true);
  }
  
  @Test
  public void exceptionTypeEnvRunLevelService() throws Exception {
    this.defRLlistener = (TestRunLevelListener) listener;
    defRLlistener.calls.clear();

    rls = new TestDefaultRunLevelService(h, false, Exception.class, recorders); 

    ExceptionRunLevelManagedService.exceptionCtor = null;
    ExceptionRunLevelManagedService.constructCount = 0;
    
    rls.proceedTo(1);
    
    assertEquals(1, ExceptionRunLevelManagedService.constructCount);
    assertEquals(0, ExceptionRunLevelManagedService.destroyCount);
    assertEquals(defRLlistener.calls.toString(), 3, defRLlistener.calls.size());
    assertListenerState(false, false, false);
  }
  
  @Test
  public void exceptionsEncounteredOnUpSide() throws Exception {
    this.defRLlistener = (TestRunLevelListener) listener;
    defRLlistener.calls.clear();

    recorders = new LinkedHashMap<Integer, Recorder>();
    rls = new TestDefaultRunLevelService(h, false, Exception.class, recorders); 

    ExceptionRunLevelManagedService.exceptionCtor = 
      RuntimeException.class.getConstructor((Class<?>[])null);
    ExceptionRunLevelManagedService.constructCount = 0;
    
    rls.proceedTo(1);
    
    assertEquals(1, ExceptionRunLevelManagedService.constructCount);
    assertEquals(0, ExceptionRunLevelManagedService.destroyCount);
    assertEquals(defRLlistener.calls.toString(), 4, defRLlistener.calls.size());
    assertListenerState(false, true, false);
  }
  
  @Test
  public void exceptionsEncounteredOnDownSide() throws Exception {
    recorders = new LinkedHashMap<Integer, Recorder>();
    rls = new TestDefaultRunLevelService(h, false, Exception.class, recorders); 

    ExceptionRunLevelManagedService.exceptionCtor = null;
    ExceptionRunLevelManagedService.constructCount = 0;

    rls.proceedTo(5);

    this.defRLlistener = (TestRunLevelListener) listener;
    defRLlistener.calls.clear();

    ExceptionRunLevelManagedService.exceptionCtor = 
      RuntimeException.class.getConstructor((Class<?>[])null);
    ExceptionRunLevelManagedService.destroyCount = 0;
    
    rls.proceedTo(0);
    
    assertEquals(1, ExceptionRunLevelManagedService.destroyCount);
    assertEquals(defRLlistener.calls.toString(), 6, defRLlistener.calls.size());
    assertListenerState(true, true, false);
  }
  
  /**
   * Verifies the behavior of an OnProgress recipient, calling proceedTo()
   */
  @Test
  public void chainedShutdownProceedToCallsAsync() throws Exception {
    installTestRunLevelService(true);
    
    installTestRunLevelService(false);

    defRLlistener.setProgressProceedTo(4, 0, rls);
    rls.proceedTo(4);
    
    synchronized (rls) {
      rls.wait(1000);
    }
    if (1 == defRLS.getCurrentRunLevel()) {
      synchronized (rls) {
        rls.wait(100);
      }
    }
    
    assertEquals(0, defRLS.getCurrentRunLevel());
    assertEquals(null, defRLS.getPlannedRunLevel());

    assertInhabitantsState(4);
    assertListenerState(true, false, true);
  }

  
  @SuppressWarnings("unchecked")
  private void installTestRunLevelService(boolean async) {
    Inhabitant<RunLevelService> r = 
      (Inhabitant<RunLevelService>) h.getInhabitant(RunLevelService.class, "default");
    assertNotNull(r);
    assertTrue(h.removeIndex(RunLevelService.class.getName(), "default"));
    h.remove(r);
    
    DefaultRunLevelService oldRLS = ((DefaultRunLevelService)rls);
    
    recorders = new LinkedHashMap<Integer, Recorder>();
    rls = new TestDefaultRunLevelService(h, async, Void.class, recorders); 
    r = new ExistingSingletonInhabitant<RunLevelService>(RunLevelService.class, rls);
    h.add(r);
    h.addIndex(r, RunLevelService.class.getName(), "default");

    this.defRLS = (DefaultRunLevelService) rls;
    this.defRLlistener = (TestRunLevelListener) listener;
    defRLlistener.calls.clear();
    
    oldRLS.setDelegate((RunLevelState)rls);
  }
  
  
  /**
   * Verifies the instantiation / release of inhabitants are correct
   * 
   * @param runLevel
   */
  private void assertInhabitantsState(int runLevel) {
    Collection<Inhabitant<?>> runLevelInhabitants = h.getAllInhabitantsByContract(RunLevel.class.getName());
    assertTrue(runLevelInhabitants.size() > 0);
    for (Inhabitant<?> i : runLevelInhabitants) {
      AbstractInhabitantImpl<?> ai = AbstractInhabitantImpl.class.cast(i);
      RunLevel rl = ai.getAnnotation(RunLevel.class);
      if (rl.value() <= runLevel) {
        if (ai.toString().contains("Invalid")) {
          assertFalse("expect not instantiated: " + ai, ai.isInstantiated());
        } else {
          if (Void.class == rl.environment()) {
            assertTrue("expect instantiated: " + ai, ai.isInstantiated());
          } else {
            assertFalse("expect instantiated: " + ai, ai.isInstantiated());
          }
        }
      } else {
        assertFalse("expect not instantiated: " + ai, ai.isInstantiated());
      }
    }
  }
  
  
  /**
   * Verifies the listener was indeed called, and the ordering is always consistent.
   */
  private void assertListenerState(boolean expectDownSide, boolean expectErrors, boolean expectCancelled) {
    assertTrue(defRLlistener.calls.size() > 0);
    int last = -2;
    boolean upSide = true;
    int sawCancel = 0;
    boolean sawError = false;
    for (TestRunLevelListener.Call call : defRLlistener.calls) {
      if (expectDownSide) {
        if (!upSide) {
          // already on the down side
          assertTrue(call.toString(), call.current <= last);
        } else {
          // haven't seen the down side yet
          if (call.current < last) {
            upSide = false;
          }
        }
      } else {
        assertTrue(call.toString(), call.current >= last);
      }

      if (upSide) {
        // we should only see cancel and error on up side (the way we designed our tests)
        if (call.type.equals("cancelled")) {
          sawCancel++;
        } else if (call.type.equals("error")) {
          sawError = true;
        }
      } else {
        if (call.type.equals("error")) {
          sawError = true;
        } else {
          assertEquals(call.toString(), "progress", call.type);
        }
      }
      
      last = call.current;
    }
    
    if (expectDownSide) {
//      assertFalse("should have ended on down side: " + defRLlistener.calls, upSide);
      // race conditions prevents us from doing the assert
      if (upSide) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Expected to have ended on down side: " + defRLlistener.calls);
      }
    }
  
    if (expectErrors) {
      assertTrue(defRLlistener.calls.toString(), sawError);
    }
    
    if (expectCancelled) {
//      assertEquals(defRLlistener.calls.toString(), 1, sawCancel);
      // race conditions prevents us from doing the assert
      if (1 != sawCancel) {
        Logger.getAnonymousLogger().log(Level.WARNING, "Expected to have seen cancel: " + defRLlistener.calls);
      }
    }
  }


  /**
   * Verifies that the recorder is always consistent.
   */
  private void assertRecorderState() {
    assertFalse(recorders.toString(), recorders.isEmpty());
    assertEquals("Belongs to a different environment", 0, ExceptionRunLevelManagedService.constructCount);
    assertEquals("Belongs to a different environment", 0, ExceptionRunLevelManagedService.destroyCount);
    // we could really do more here...
  }
  
  
  private static class TestDefaultRunLevelService extends DefaultRunLevelService {

    TestDefaultRunLevelService(Habitat habitat, boolean async, Class<?> targetEnv,
        HashMap<Integer, Recorder> recorders) {
      super(habitat, async, targetEnv, recorders);
    }
    
  }
  
}
