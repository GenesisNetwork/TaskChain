/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.taskchain;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.iluminary.dungeonapi.hybrid.database.DatabaseConnection;

@SuppressWarnings("WeakerAccess")
public class TaskChainTasks {
	
	/**
     * Generic task with synchronous return (but may execute on any thread)
     *
     * @param <R>
     * @param <A>
     */
    public interface Task<R, A> {
    	
    	public static final Object NO_RETURN_VALUE = new Object();
    	
        /**
         * @see TaskChain#getCurrentChain()
         */
        default TaskChain<?> getCurrentChain() {
            return TaskChain.getCurrentChain();
        }

        R run(A input) throws Throwable;
    }

    /**
     * A task that expects no input, and returns a value.
     * Likely to be the first task in the chain
     */
    public interface SupplierTask<R> extends Task<R, Object> {
        @Override
        default R run(Object input) throws Throwable{
            return run();
        }

        R run() throws Throwable;
    }


    /**
     * A task that expects input, but will not provide a response.
     * Likely to be the last task in the chain
     * @param <A>
     */
    public interface ConsumerTask<A> extends Task<Object, A> {
        @Override
        default Object run(A input) throws Throwable{
            runLast(input);
            return NO_RETURN_VALUE;
        }

        void runLast(A input) throws Throwable;
    }

    /**
     * A task that expects no input or output
     */
    public interface GenericTask extends Task<Object, Object> {
        @Override
        default Object run(Object input) throws Throwable{
            runGeneric();
            return NO_RETURN_VALUE;
        }

        void runGeneric() throws Throwable;
    }

    

    /**
     * A task that returns a future to be completed later. Takes input from chain and when the future completes,
     * the value will be passed to the next task.
     *
     * @param <R> The type the Future will complete with
     * @param <A> The type from the previous task
     */
    public interface FutureTask<R, A> extends Task<R, A> {
        @Override
        default R run(A input) throws Throwable{
            throw new UnsupportedOperationException("Unable to execute futuretask immediatly");
        }

        CompletableFuture<R> runFuture(A input) throws Throwable;
    }

    /**
     * A Task that returns a future to be completed later. Expects no input, and  when the future completes,
     * the value will be passed to the next task.
     */
    public interface FutureSupplierTask<R> extends FutureTask<R, Object> {
        @Override
        default CompletableFuture<R> runFuture(Object input) throws Throwable{
            return runFuture();
        }

        CompletableFuture<R> runFuture() throws Throwable;
    }


    /**
     * A generic task that expects no input or output, but uses a future style API
     * to control moving to the next task.
     */
    public interface FutureGenericTask extends FutureTask<Object, Object>{
        @Override
        default CompletableFuture<Object> runFuture(Object input) throws Throwable{
            return runFuture();
        }

        CompletableFuture<Object> runFuture() throws Throwable;
    }
    
    
    
    public interface DatabaseTask<R, A> extends Task<R, A> {
    	@Override
        default R run(A input) throws Throwable{
            throw new UnsupportedOperationException("Unable to execute databasetask directly");
        }
    	
        default R runDatabase(A input, boolean commit) throws Throwable{
            try(DatabaseConnection con = new DatabaseConnection()){
            	R output = runDatabase(con, input);
            	
            	if(commit) {
            		con.commit();
            	}
            	
            	return output;
            }
        }

        R runDatabase(DatabaseConnection con, A input) throws Throwable;
    }
    
    public interface DatabaseSupplierTask<R> extends DatabaseTask<R, Object> {
    	@Override
        default R runDatabase(DatabaseConnection con, Object input) throws Throwable{
            return runDatabaseSupplier(con);
        }

        R runDatabaseSupplier(DatabaseConnection con) throws Throwable;
    }
    
    public interface DatabaseConsumerTask<A> extends DatabaseTask<Object, A> {
    	@Override
        default Object runDatabase(DatabaseConnection con, A input) throws Throwable{
    		runDatabaseConsumer(con, input);
            return NO_RETURN_VALUE;
        }

        void runDatabaseConsumer(DatabaseConnection con, A input) throws Throwable;
    }
    
    public interface DatabaseGenericTask extends DatabaseTask<Object, Object> {
    	@Override
        default Object runDatabase(DatabaseConnection con, Object input) throws Throwable{
    		runDatabaseGeneric(con);
            return NO_RETURN_VALUE;
        }

        void runDatabaseGeneric(DatabaseConnection con) throws Throwable;
    }


    

    /**
     * A task that does not return immediately. A supplied Consumer controls when
     * the chain should proceed to the next task, and providing a response to be passed
     * to the next task. This is a Callback style API in relation to the Future based API.
     */
    public interface AsyncExecutingTask<R, A> extends Task<R, A> {
        @Override
        default R run(A input) throws Throwable{
            throw new UnsupportedOperationException("Unable to execute AsyncExecutingTask immediatly");
        }

        void runAsync(A input, Consumer<R> next) throws Throwable;
    }

    /**
     * @see AsyncExecutingTask
     * @see SupplierTask
     */
    public interface AsyncExecutingSupplierTask<R> extends AsyncExecutingTask<R, Object> {

        @Override
        default void runAsync(Object input, Consumer<R> next) throws Throwable{
            run(next);
        }

        void run(Consumer<R> next) throws Throwable;
    }

    /**
     * @see AsyncExecutingTask
     * @see GenericTask
     */
    public interface AsyncExecutingGenericTask extends AsyncExecutingTask<Object, Object> {
        @Override
        default void runAsync(Object input, Consumer<Object> next) throws Throwable{
            run(() -> next.accept(NO_RETURN_VALUE));
        }

        void run(Runnable next) throws Throwable;
    }
}
